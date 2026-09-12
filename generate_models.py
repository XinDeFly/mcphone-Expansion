from bbmodel import Model
from pathlib import Path

OUT = Path("src/main/resources/assets/generated_mod/models")
OUT.mkdir(parents=True, exist_ok=True)


def terminal(name, texture, accent, top_detail):
    m = Model(name, tex_size=16)
    m.region("body", 0, 0, 16, 16)
    m.fill("body", (38, 48, 61))
    m.paint("body", lambda u, v, w, h: accent if (u in (0, w - 1) or v in (0, h - 1)) else None)
    m.cube("base", (1, 0, 1), (15, 3, 15), tex="body")
    m.cube("case", (2, 3, 2), (14, 14, 14), tex="body")
    m.cube("screen", (3, 8, 0.85), (13, 13, 2.0), tex="body", inflate=0.01)
    m.cube("top", (3, 14, 3), (13, 16, 13), tex="body")
    m.cube("top_mark", (6, 15.8, 6), (10, 16.2, 10), tex="body", inflate=0.01, rotation=(0, 45, 0), origin=(8, 16, 8))
    m.save(str(OUT / "block" / f"{name}.bbmodel"), model_format="java_block")
    lo, hi = m.bounds()
    print(name, tuple(lo), tuple(hi))

terminal("trading_platform", "trading_platform", (198, 157, 48), (60, 194, 143))
terminal("public_market", "public_market", (54, 172, 145), (235, 194, 70))
terminal("trade_counter", "trade_counter", (176, 116, 48), (222, 177, 62))
