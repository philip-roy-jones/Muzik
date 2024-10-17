schema ={
    "randomArtists": {
        "_id": "g87430985709gh8374",
        "spotifyId": "spotify:artist:1",
        "name": "artist1",
        // "albums": ["spotify:album:1"]           // Not storing this as an artist can add albums in the future
        "images": {

        }
    },
    "randomAlbums": {
        "_id": "66f9c144f64e490e9be228f9",
        "spotify_id": "spotify:album:1",
        "type": "album",                            // album, single, compilation
        "name": "album1",
        // "artist_oids": ["h364n05987346hn5g03"],
        "release_date": "2019",
        "release_date_precision": "year",
        "total_tracks": 2,
        // "track_oids": [
        //     "fdsf87h485239h20",
        //     "dfs98h650984"
        // ],
        "images": [
            {
            "url": "https://i.scdn.co/image/ab67616d0000b2731dacfbc31cc873d132958af9",
            "height": 640,
            "width": 640
            },
            {
            "url": "https://i.scdn.co/image/ab67616d00001e021dacfbc31cc873d132958af9",
            "height": 300,
            "width": 300
            },
            {
            "url": "https://i.scdn.co/image/ab67616d000048511dacfbc31cc873d132958af9",
            "height": 64,
            "width": 64
            }
        ]
    },
    "randomTracks":{
        "_id": "g87430985709gh8374",
        "spotifyId": "spotify:track:1",
        // "artist_oids": ["7n56bg904387564"],
        "artistName": ["artist1", "artist2"],
        "album_oid": "43h75n0934857340",
        "name": "track1",
        "explicit": false,
        "albumOid": "g87430985709gh8374"
    },
    // This only exists because tracks don't have images
    "randomTrackAlbums":{
        "_id": "g87430985709gh8374",
        "spotifyId": "spotify:album:1",
        "images": [
            {
              "url": "https://i.scdn.co/image/ab67616d0000b2736aa1a0eae5f023675b4e9818",
              "height": 640,
              "width": 640
            },
            {
              "url": "https://i.scdn.co/image/ab67616d00001e026aa1a0eae5f023675b4e9818",
              "height": 300,
              "width": 300
            },
            {
              "url": "https://i.scdn.co/image/ab67616d000048516aa1a0eae5f023675b4e9818",
              "height": 64,
              "width": 64
            }
          ],
    },
    "randomTrackFeatures":{
        "_id": "g87430985709gh8374",
        "track_oid": "43h75n0934857340",
        "danceability": 0.5,
        "energy": 0.5,
        "key": 0,
        "loudness": -60,
        "mode": 0,
        "speechiness": 0.5,
        "acousticness": 0.5,
        "instrumentalness": 0.5,
        "liveness": 0.5,
        "valence": 0.5,
        "tempo": 0.5,
        "duration_ms": 1000,
        "time_signature": 4,
        "expires_at": "2025-01-01T00:00:00Z"
    },
    "users": {
        "_id": "g87430985709gh8374",
        "username": "user1",
        "firstName": "John",
        "lastName": "Doe",
        "email": "test@iamthemuzik.com",
        "password": "dsf9843h9v7834g45398",
        "connections": {
            "spotify": {
                "refreshToken": "fj43298rf423j98h",
                "refreshTokenIssueDate": "2024-09-01T00:00:00Z"
            }
        },
        "created_at": "2024-09-01T00:00:00Z",
        "updated_at": "2024-09-01T00:00:00Z"
    },
    "popularity": {
        "spotify:track:1": {
            "2024-09-01": 75,
            "2024-09-02": 80,
            "2024-09-03": 81
        }
    }
}