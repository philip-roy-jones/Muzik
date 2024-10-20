import os
import requests
from dotenv import load_dotenv
from pathlib import Path
from datetime import datetime

env_path = Path(__file__).resolve().parents[2] / '.env'
load_dotenv(dotenv_path=env_path)

def get_name_of_tracks(query, token):
    url = "https://api.spotify.com/v1/search"
    headers = {
        "Authorization": f"Bearer {token}"
    }
    params = {
        "q": query,
        "type": "track",
        "limit": 50
    }
    start_time = datetime.now()
    response = requests.get(url, headers=headers, params=params)
    end_time = datetime.now()
    response_time_ms = (end_time - start_time).total_seconds() * 1000

    if response.status_code == 200:
        tracks = response.json().get('tracks', {}).get('items', [])
        total_tracks = response.json().get('tracks', {}).get('total', 0)

        print(f"Response time: {response_time_ms:.2f} ms")
        log_total_tracks(total_tracks)
        log_track_names(tracks)

        print(f"Tracks found: {total_tracks}")
    else:
        response.raise_for_status()
    return None

def log_total_tracks(total_tracks):
    date_str = datetime.now().strftime("%Y-%m-%d")
    log_entry = f"{date_str},{total_tracks}\n"
    log_file_path = Path(__file__).resolve().parents[2] / "tools" / "Test" / "results" / "track_log.csv"
    log_file_path.parent.mkdir(parents=True, exist_ok=True)  # Ensure the directory exists

    # Check if the file exists to add a header if it doesn't
    if not log_file_path.exists():
        with log_file_path.open("w", encoding="utf-8") as log_file:
            log_file.write("Date,Total Tracks\n")

    with log_file_path.open("a", encoding="utf-8") as log_file:
        log_file.write(log_entry)

def log_track_names(tracks):
    date_str = datetime.now().strftime("%Y-%m-%d")
    log_file_path = Path(__file__).resolve().parents[2] / "tools" / "Test" / "results" / "track_names.csv"
    log_file_path.parent.mkdir(parents=True, exist_ok=True)  # Ensure the directory exists

    # Check if the file exists to add a header if it doesn't
    if not log_file_path.exists():
        with log_file_path.open("w", encoding="utf-8") as log_file:
            log_file.write("Date,Track Name\n")

    with log_file_path.open("a", encoding="utf-8") as log_file:
        for track in tracks:
            track_name = track.get('name', 'Unknown')
            log_entry = f"{date_str},{track_name}\n"
            log_file.write(log_entry)

if __name__ == "__main__":
    query = "вДц"
    token = os.getenv("SPOTIFY_ACCESS_TOKEN")
    test_token = os.getenv("TEST_SP")

    get_name_of_tracks(query, token)