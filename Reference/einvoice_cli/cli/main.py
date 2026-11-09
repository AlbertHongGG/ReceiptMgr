"""
einvoice_cli.cli.main
=====================
Typer CLI 命令列入口模組。
提供手機條碼發票表頭清單、單筆品項明細、金鑰檢查與 .runtime 審計歷程等 4 大純淨核心命令。
"""

import sys
from pathlib import Path
from typing import Optional
import typer
from rich.console import Console

from ..config import settings, RUNTIME_DIR
from ..crypto.session import SessionKeyRepository
from ..models.comms import CarrierComms
from ..models.requests import InvoiceHeaderReqData, InvoiceDetailReqData
from ..client.client import EInvoiceClient
from ..client.exceptions import EInvoiceError
from .formatters import (
    render_header_response,
    render_detail_response,
    render_sessions,
    render_json,
)

# 解決 Windows 終端編碼問題
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

app = typer.Typer(
    name="einvoice",
    help="財政部電子發票 (UPI 體系) 官方端口逆向模擬與快速測試 CLI 工具",
    no_args_is_help=True,
    add_completion=False,
)
console = Console()


def _get_client(
    key_id: int,
    custom_session_key: Optional[str],
    env_file: Optional[str],
) -> EInvoiceClient:
    """建構已綁定 Session Key 之 API 客戶端"""
    if custom_session_key:
        return EInvoiceClient(
            session_key=custom_session_key,
            session_key_id="custom",
        )

    repo = SessionKeyRepository.load(custom_path=env_file)
    if not repo.keys:
        console.print("[bold red]錯誤：找不到任何可用的 Session Key！[/bold red]")
        console.print(f"請確認設定檔 [{repo.source_path}] 存在並包含有效金鑰，或使用 --session-key 直接傳入。")
        raise typer.Exit(code=1)

    try:
        sk_bytes = repo.get_key(key_id)
    except Exception as e:
        console.print(f"[bold red]載入金鑰 ID {key_id} 失敗：{e}[/bold red]")
        raise typer.Exit(code=1)

    return EInvoiceClient(
        session_key=sk_bytes,
        session_key_id=key_id,
    )


@app.command("header", help="【端口 1】查詢手機條碼發票表頭清單 (query-invoices-header)")
def query_header(
    start: str = typer.Option("2026/04/26", "--start", "-s", help="查詢起始日期 YYYY/MM/DD"),
    end: str = typer.Option("2026/09/05", "--end", "-e", help="查詢結束日期 YYYY/MM/DD"),
    barcode: str = typer.Option(settings.default_barcode, "--barcode", "-b", help="手機條碼載具號碼"),
    only_winning: str = typer.Option("A", "--only-winning", "-w", help="中獎篩選 (Y:僅中獎, N:僅未中獎, A:全部)"),
    page: int = typer.Option(1, "--page", "-p", help="分頁頁碼"),
    key_id: int = typer.Option(1, "--key-id", "-k", help="Session Key 編號 (1 或 2)"),
    session_key: Optional[str] = typer.Option(None, "--session-key", help="直接提供自訂 Session Key"),
    liat: Optional[int] = typer.Option(None, "--liat", help="指定登入時間戳 (預設取用設定值)"),
    json_output: bool = typer.Option(False, "--json", help="以原始結構化 JSON 輸出至終端"),
    env_file: Optional[str] = typer.Option(None, "--env-file", help="指定 .env 檔案路徑 (預設為 Reference/.env)"),
):
    """
    發送 query-invoices-header 請求並自動於 .runtime 記錄原生 JSON 審計檔。
    """
    client = _get_client(key_id, session_key, env_file)

    try:
        req_data = InvoiceHeaderReqData(
            cardNo=barcode,
            startDate=start,
            endDate=end,
            onlyWinningInv=only_winning.upper(),
            page=page,
        )
    except Exception as e:
        console.print(f"[bold red]請求參數校驗失敗：{e}[/bold red]")
        raise typer.Exit(code=1)

    comms = CarrierComms.create_fresh(
        appid=settings.default_appid,
        barcode=barcode,
        liat=liat,
    )

    cmd_desc = f"einvoice header -s {start} -e {end} -b {barcode} -w {only_winning} -p {page} -k {key_id}"

    with console.status("[bold cyan]正在連線財政部 UPI 伺服器查詢發票表頭...[/bold cyan]"):
        try:
            resp = client.query_headers(req_data, comms=comms, command_desc=cmd_desc)
        except EInvoiceError as e:
            console.print(f"[bold red]查詢失敗：{e}[/bold red]")
            if client.last_audit_file:
                console.print(f"審計紀錄已輸出至: {client.last_audit_file}")
            raise typer.Exit(code=1)

    if json_output:
        render_json(resp.model_dump())
    else:
        render_header_response(resp, audit_file=client.last_audit_file)


@app.command("detail", help="【端口 2】查詢單筆發票消費品項明細 (query-invoices-details)")
def query_detail(
    inv_num: str = typer.Option("GW38470545", "--inv-num", "-n", help="目標發票字軌號碼 (如 GW38470545)"),
    inv_date: str = typer.Option("2026/11/05", "--inv-date", "-d", help="發票開立日期 YYYY/MM/DD"),
    barcode: str = typer.Option(settings.default_barcode, "--barcode", "-b", help="手機條碼載具號碼"),
    key_id: int = typer.Option(1, "--key-id", "-k", help="Session Key 編號 (1 或 2)"),
    session_key: Optional[str] = typer.Option(None, "--session-key", help="直接提供自訂 Session Key"),
    liat: Optional[int] = typer.Option(None, "--liat", help="指定登入時間戳 (預設取用設定值)"),
    json_output: bool = typer.Option(False, "--json", help="以原始結構化 JSON 輸出至終端"),
    env_file: Optional[str] = typer.Option(None, "--env-file", help="指定 .env 檔案路徑 (預設為 Reference/.env)"),
):
    """
    發送 query-invoices-details 請求並自動於 .runtime 記錄原生 JSON 審計檔。
    """
    client = _get_client(key_id, session_key, env_file)

    try:
        req_data = InvoiceDetailReqData(
            cardNo=barcode,
            invNum=inv_num,
            invDate=inv_date,
        )
    except Exception as e:
        console.print(f"[bold red]請求參數校驗失敗：{e}[/bold red]")
        raise typer.Exit(code=1)

    comms = CarrierComms.create_fresh(
        appid=settings.default_appid,
        barcode=barcode,
        liat=liat,
    )

    cmd_desc = f"einvoice detail -n {inv_num} -d {inv_date} -b {barcode} -k {key_id}"

    with console.status(f"[bold cyan]正在連線財政部 UPI 伺服器查詢發票明細 ({inv_num})...[/bold cyan]"):
        try:
            resp = client.query_details(req_data, comms=comms, command_desc=cmd_desc)
        except EInvoiceError as e:
            console.print(f"[bold red]查詢失敗：{e}[/bold red]")
            if client.last_audit_file:
                console.print(f"審計紀錄已輸出至: {client.last_audit_file}")
            raise typer.Exit(code=1)

    if json_output:
        render_json(resp.model_dump())
    else:
        render_detail_response(resp, audit_file=client.last_audit_file)


@app.command("sessions", help="檢驗並列出目前載入之 Session Keys 狀態 (來自 Reference/.env)")
def list_sessions(
    env_file: Optional[str] = typer.Option(None, "--env-file", help="指定 .env 檔案路徑 (預設為 Reference/.env)"),
):
    """檢驗 Reference/.env 中的金鑰格式與載入數量"""
    repo = SessionKeyRepository.load(custom_path=env_file)
    if not repo.keys:
        console.print(f"[bold red]未於 [{repo.source_path}] 偵測到任何 Session Key。[/bold red]")
        return
    console.print(f"[dim]載入來源: {repo.source_path}[/dim]")
    render_sessions(repo)


@app.command("history", help="瀏覽 .runtime 目錄下的審計紀錄列表")
def list_history(
    limit: int = typer.Option(10, "--limit", "-l", help="顯示最新筆數"),
):
    """列出 .runtime 目錄中的執行日誌檔案"""
    if not RUNTIME_DIR.exists():
        console.print("[yellow]尚未建立 .runtime 目錄，請先執行任何查詢。[/yellow]")
        return

    json_files = sorted(RUNTIME_DIR.glob("*.json"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not json_files:
        console.print("[yellow].runtime 目錄下無任何審計檔案。[/yellow]")
        return

    from rich.table import Table
    table = Table(title=f"審計紀錄歷史 (最新 {min(limit, len(json_files))} 筆)", show_lines=True)
    table.add_column("序號", justify="center", style="dim", width=6)
    table.add_column("檔案名稱 (yyyymmdd_hhmmss_api名稱.json)", justify="left", width=46)
    table.add_column("大小", justify="right", width=10)
    table.add_column("修改時間", justify="center", width=20)

    from datetime import datetime
    for idx, f in enumerate(json_files[:limit], start=1):
        mtime = datetime.fromtimestamp(f.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S")
        size_kb = f"{round(f.stat().st_size / 1024, 1)} KB"
        table.add_row(str(idx), f.name, size_kb, mtime)

    console.print(table)


if __name__ == "__main__":
    app()
