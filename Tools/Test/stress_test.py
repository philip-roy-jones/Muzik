import requests
import os
from dotenv import load_dotenv
from pathlib import Path
import time
import random

# Load environment variables from a .env file located two directories back
env_path = Path(__file__).resolve().parents[2] / '.env'
load_dotenv(dotenv_path=env_path)

def send_request(url):
    headers = {
        "Authorization": f"Bearer {os.getenv('SPOTIFY_ACCESS_TOKEN')}"
    }
    response = requests.get(url, headers=headers)
    return response

def main():
    num_requests = 0
    status_code = 0
    milliseconds = 0
    delay = milliseconds / 1000

    while status_code != 429:
        total_tracks = 0
        num_requests += 1  # Increment request count

        id = "1Cs0zKBU1kc0i8ypK3B9ai"
        response = send_request(f"https://api.spotify.com/v1/artists/{id}/albums?include_groups=single")
        time.sleep(delay)
        status_code = response.status_code
        print(f"Request {num_requests} - Status code: {status_code}")

        total_singles = response.json().get("total")
        total_tracks += total_singles

        response = send_request(f"https://api.spotify.com/v1/artists/{id}/albums?include_groups=album")
        time.sleep(delay)
        status_code = response.status_code
        print(f"Request {num_requests} - Status code: {status_code}")

        albums = response.json().get("items")
        for album in albums:
            tracks_in_album = album.get("total_tracks")
            total_tracks += tracks_in_album

        # Generate a random number from 1 to the number of total tracks
        random_track_number = random.randint(0, total_tracks - 1)
        print(f"Random track number: {random_track_number}")

        # if random_track_number <= total_singles - 1:
        #     print("Choose a single")
        # else:
        #     print("Choose an album")

        # album_id = "2Sq9AIsIrad2GygwB6QLPt"
        # response = send_request(f"https://api.spotify.com/v1/albums/{album_id}/tracks")
        # time.sleep(delay)
        # status_code = response.status_code
        # print(f"Request {num_requests} - Status code: {status_code}")

    print(f"Total requests sent: {num_requests}")

if __name__ == "__main__":
    main()