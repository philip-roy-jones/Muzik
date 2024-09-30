import asyncio
import aiohttp
import os
from dotenv import load_dotenv
from pathlib import Path

# Load environment variables from a .env file located two directories back
env_path = Path(__file__).resolve().parents[2] / '.env'
load_dotenv(dotenv_path=env_path)

async def send_request(session):
    url = "https://api.spotify.com/v1/tracks/1R7eMSKgatyWobFUdbXgpL"
    headers = {
        "Authorization": f"Bearer {os.getenv('SPOTIFY_ACCESS_TOKEN')}"
    }
    async with session.get(url, headers=headers) as response:
        return response.status

async def main():
    num_requests = 0
    status_code = 0

    async with aiohttp.ClientSession() as session:
        while status_code != 429:
            num_requests += 1  # Increment request count
            # Start the request but don't wait for it to finish
            request_task = asyncio.create_task(send_request(session))
            
            # Wait for 4 seconds
            await asyncio.sleep(3)
            
            # Now await the request task
            status_code = await request_task
            print(f"Request {num_requests} - Status code: {status_code}")

    print(f"Total requests sent: {num_requests}")

asyncio.run(main())