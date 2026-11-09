"""
einvoice_cli.__main__
=====================
支援 python -m einvoice_cli 直接執行。
"""

from .cli.main import app

if __name__ == "__main__":
    app()
