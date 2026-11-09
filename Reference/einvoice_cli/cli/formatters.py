"""
einvoice_cli.cli.formatters
===========================
Rich 終端美化輸出器。
提供表格、面板、狀態指示與 JSON 高亮展示。
"""

import json
from typing import Any
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.syntax import Syntax

from ..models.responses import InvoiceHeaderResponse, InvoiceDetailResponse
from ..crypto.session import SessionKeyManager

console = Console()


def render_header_response(resp: InvoiceHeaderResponse, audit_file: str | None = None) -> None:
    """渲染發票表頭清單表格"""
    # 狀態與摘要面板
    status_style = "bold green" if resp.is_success else "bold red"
    panel_text = f"[{status_style}]伺服器回應: [{resp.code}] {resp.msg}[/{status_style}]\n"
    panel_text += f"查詢總筆數: [cyan]{len(resp.details)}[/cyan] 筆"
    if audit_file:
        panel_text += f"\n審計檔案: [dim]{audit_file}[/dim]"

    console.print(Panel(panel_text, title="【端口 1: query-invoices-header 查詢結果】", expand=False))

    if not resp.details:
        console.print("[yellow]查無發票資料。[/yellow]")
        return

    table = Table(title="發票清單", show_lines=True)
    table.add_column("序號", justify="center", style="dim", width=6)
    table.add_column("期別", justify="center", width=8)
    table.add_column("發票號碼", justify="center", style="bold cyan", width=12)
    table.add_column("開立日期/時間", justify="center", width=18)
    table.add_column("賣方營業人", justify="left", width=22)
    table.add_column("金額", justify="right", style="bold green", width=10)
    table.add_column("狀態", justify="center", width=12)
    table.add_column("中獎資訊", justify="center", style="bold magenta", width=14)

    for item in resp.details:
        date_str = item.formatted_inv_date
        time_str = item.invoiceTime or ""
        full_dt = f"{date_str} {time_str}".strip()

        award_str = "未中獎"
        if item.awardInfo:
            if isinstance(item.awardInfo, dict):
                p_amt = str(item.awardInfo.get("prizeAmt", "")).strip()
                award_str = f"獎金 ${p_amt}" if p_amt else "中獎"
            else:
                p_amt = str(item.awardInfo.prizeAmt or "").strip()
                award_str = f"獎金 ${p_amt}" if p_amt else "中獎"

        table.add_row(
            item.rowNum,
            item.invPeriod,
            item.invNum,
            full_dt,
            item.sellerName or item.sellerBan or "-",
            f"${item.amount}",
            item.invStatus or "-",
            award_str
        )

    console.print(table)


def render_detail_response(resp: InvoiceDetailResponse, audit_file: str | None = None) -> None:
    """渲染單筆發票品項明細表格"""
    status_style = "bold green" if resp.is_success else "bold red"
    panel_text = f"[{status_style}]伺服器回應: [{resp.code}] {resp.msg}[/{status_style}]\n"
    panel_text += f"發票號碼: [bold cyan]{resp.invNum}[/bold cyan]  期別: [cyan]{resp.invPeriod}[/cyan]\n"
    panel_text += f"開立日期: {resp.invDate} {resp.invoiceTime or ''}\n"
    panel_text += f"營業人: [bold]{resp.sellerName or '-'}[/bold] (統編: {resp.sellerBan or '-'})\n"
    panel_text += f"地址: [dim]{resp.sellerAddress or '-'}[/dim]\n"
    panel_text += f"總金額: [bold green]${resp.amount or '0'}[/bold green]"
    if audit_file:
        panel_text += f"\n審計檔案: [dim]{audit_file}[/dim]"

    console.print(Panel(panel_text, title="【端口 2: query-invoices-details 發票明細】", expand=False))

    if not resp.details:
        console.print("[yellow]此發票無品項明細資訊。[/yellow]")
        return

    table = Table(title=f"品項明細 ({resp.invNum})", show_lines=True)
    table.add_column("項次", justify="center", style="dim", width=6)
    table.add_column("品名描述", justify="left", width=36)
    table.add_column("數量", justify="right", width=8)
    table.add_column("單價", justify="right", width=10)
    table.add_column("小計金額", justify="right", style="bold green", width=12)

    for item in resp.details:
        table.add_row(
            item.rowNum,
            item.description,
            item.quantity,
            f"${item.unitPrice}",
            f"${item.amount}"
        )

    console.print(table)


def render_sessions(mgr: SessionKeyManager) -> None:
    """列出可用金鑰狀態"""
    keys_info = mgr.list_keys_info()
    table = Table(title="載入之 Session Keys 狀態清單", show_lines=True)
    table.add_column("金鑰 ID", justify="center", style="bold cyan", width=10)
    table.add_column("遮蔽金鑰字串", justify="center", width=24)
    table.add_column("字元長度", justify="center", width=10)
    table.add_column("格式驗證", justify="center", width=12)

    for k in keys_info:
        valid_mark = "[bold green]通過 (43 字元)[/bold green]" if k["valid_format"] else "[bold red]異常[/bold red]"
        table.add_row(str(k["id"]), k["masked"], str(k["length"]), valid_mark)

    console.print(table)


def render_json(data: Any) -> None:
    """美化輸出原生 JSON"""
    json_str = json.dumps(data, indent=2, ensure_ascii=False)
    syntax = Syntax(json_str, "json", theme="monokai", line_numbers=True)
    console.print(syntax)
