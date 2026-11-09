# 財政部電子發票 (UPI 體系) 官方端口逆向模擬與快速測試 CLI 工具 (`einvoice-cli`)

本專案位於 `ReceiptMgr/Reference` 目錄，專為模擬與快速測試財政部電子發票雲端服務 (UPI 體系) 兩大核心查詢端口所開發。採用純 Python `uv` 現代工具鏈、完全物件導向 (OOP) 與 Pydantic v2 強型別架構打造，絕無修修補補或將就之過渡代碼。

每次執行 API 呼叫，均會自動於專案根目錄 `.runtime` 產生標準格式的審計記錄，檔名嚴格遵循 `yyyymmdd_hhmmss_api名稱.json`，且請求與回應均為完整原生 JSON 樹狀物件（非轉義字串）。

---

## 核心功能與支援端口

1. **端口 1**：`POST /einvoice/carriers/query-invoices-header`
   - 手機條碼載具發票表頭清單查詢（支援日期範圍、中獎發票篩選、分頁）。
   - 自動相容伺服器多型回傳架構（頂層物件或 `carrierQueryList` 包覆）。
2. **端口 2**：`POST /einvoice/carriers/query-invoices-details`
   - 單筆發票消費品項明細查詢（含發票號碼、營業人統編/地址、各項次單價、數量與小計）。
3. **Session Key 安全管理**：
   - 唯一限定自 `Reference/.env` 讀取金鑰，完全封閉獨立。
   - 支援 `--key-id 1|2` 切換或 `--session-key` 動態指定，自動進行 43 字元格式驗證與日誌脫敏。
4. **40 秒 Nonce 窗與時效自動校準**：
   - 封裝 `CarrierComms` 自動計算 `iat` 與 `exp`（`exp - iat = 40`），並帶入登入時間戳記 (`liat`)。
5. **原生 JSON 審計引擎 (`.runtime`)**：
   - 請求與回應均以原生 JSON 字典樹狀結構保存，杜絕雙重字串序列化。

---

## 快速開始 (Quick Start)

進入 `Reference` 目錄，以 `uv` 進行操作：

```bash
cd Reference

# 1. 查看 CLI 說明與可用子指令 (純淨四大指令)
uv run einvoice --help

# 2. 檢驗目前載入之 Session Keys 狀態 (來源限定 Reference/.env)
uv run einvoice sessions

# 3. 查詢手機條碼發票清單 (端口 1)
# 預設查詢載具 /8J722KT 在 2026/04/26 ~ 2026/09/05 之發票
uv run einvoice header

# 僅篩選中獎發票
uv run einvoice header -w Y

# 4. 查詢單筆發票品項明細 (端口 2)
uv run einvoice detail -n GW38470545 -d 2026/11/05

# 查詢另一筆中獎發票之維修明細
uv run einvoice detail -n BK22807399 -d 2026/05/26

# 5. 查看 .runtime 目錄下的執行審計歷史清單
uv run einvoice history
```

---

## 命令列參數速查

### 1. `header` 指令
| 參數名稱 | 縮寫 | 預設值 | 說明 |
| :--- | :---: | :---: | :--- |
| `--start` | `-s` | `2026/04/26` | 查詢起始日期 (YYYY/MM/DD) |
| `--end` | `-e` | `2026/09/05` | 查詢結束日期 (YYYY/MM/DD) |
| `--barcode` | `-b` | `/8J722KT` | 手機條碼載具號碼 |
| `--only-winning` | `-w` | `A` | 中獎篩選 (`Y`: 僅中獎, `N`: 僅未中獎, `A`: 全部) |
| `--page` | `-p` | `1` | 查詢頁碼 |
| `--key-id` | `-k` | `1` | 使用之 Session Key 編號 (1 或 2) |
| `--session-key` | | `None` | 直接指定自訂 Session Key 字串 |
| `--env-file` | | `Reference/.env` | 指定 .env 檔案路徑 |
| `--json` | | `False` | 以純 JSON 格式輸出至終端 |

### 2. `detail` 指令
| 參數名稱 | 縮寫 | 預設值 | 說明 |
| :--- | :---: | :---: | :--- |
| `--inv-num` | `-n` | `GW38470545` | 發票字軌號碼 (2 碼英文 + 8 碼數字) |
| `--inv-date` | `-d` | `2026/11/05` | 發票開立日期 (YYYY/MM/DD) |
| `--barcode` | `-b` | `/8J722KT` | 手機條碼載具號碼 |
| `--key-id` | `-k` | `1` | 使用之 Session Key 編號 (1 或 2) |
| `--session-key` | | `None` | 直接指定自訂 Session Key 字串 |
| `--env-file` | | `Reference/.env` | 指定 .env 檔案路徑 |
| `--json` | | `False` | 以純 JSON 格式輸出至終端 |

---

## 審計記錄目錄 (`.runtime`) 說明

專案根目錄下的 `.runtime` 會記錄每一次執行的完整資料：
- **命名規範**：`yyyymmdd_hhmmss_api名稱.json`
  - 例：`20261109_132800_query-invoices-header.json`
- **結構保證**：
  - `metadata`: 執行時間、耗時、HTTP 狀態碼、調用指令、掩蔽金鑰、成功旗標。
  - `request`: 完整 URL、HTTP 標頭、解開之 JWT Header、**原生 JSON 架構之 JWT Payload (含 comms 與 reqdata)**、計算之簽章與 raw token。
  - `response`: HTTP 狀態、伺服器標頭、**原生 JSON 架構之回傳 Body (非轉義字串)**。

---

## 程式碼架構 (Clean Layered OOP)

```
Reference/einvoice_cli/
├── config.py              # 全域組態設定 (限定 Reference/.env 與 .runtime)
├── __main__.py            # 支援 python -m einvoice_cli 入口
├── models/                # Pydantic v2 強型別模型層
│   ├── comms.py           # 通訊認證上下文 (CarrierComms) 與 40s 時效驗證
│   ├── requests.py        # 端口 1 與端口 2 請求參數模型
│   ├── responses.py       # API 回應模型 (含 carrierQueryList 多型解包)
│   └── audit.py           # 審計日誌原生 JSON 資料契約模型
├── crypto/                # 密碼學與金鑰倉儲層
│   ├── signer.py          # Base64URL 與 HMAC-SHA256 簽名引擎
│   └── session.py         # SessionKeyRepository (專門載入 Reference/.env)
├── client/                # 網路通訊傳輸層
│   ├── client.py          # EInvoiceClient (HTTPX, SSL 適配, 審計管線)
│   └── exceptions.py      # EInvoiceError 階層化領域例外
├── audit/                 # 審計引擎
│   └── logger.py          # RuntimeLogger (原子性輸出 .runtime)
└── cli/                   # 終端人機介面
    ├── main.py            # Typer 命令列實作 (header / detail / sessions / history)
    └── formatters.py      # Rich 終端表格與彩色美化
```
