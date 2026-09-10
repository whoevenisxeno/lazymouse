import asyncio
import json
import math
import sys

from websockets.asyncio.client import connect


async def main(host: str, token: str):
    async with connect(f"ws://{host}:8098") as ws:
        await ws.send(json.dumps({"t": "hello", "token": token}))
        print(await ws.recv())
        for i in range(120):
            await ws.send(json.dumps(
                {"t": "m", "x": 6 * math.cos(i / 6), "y": 6 * math.sin(i / 6)}))
            await asyncio.sleep(1 / 120)
        await ws.send(json.dumps({"t": "scroll", "y": -3}))
        await ws.send(json.dumps({"t": "click", "btn": "l"}))
        print("done")


if __name__ == "__main__":
    asyncio.run(main(sys.argv[1], sys.argv[2]))
