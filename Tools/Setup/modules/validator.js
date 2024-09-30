const tracks = {
  $jsonSchema: {
    bsonType: "object",
    required: ["track_id", "album_oid", "name", "explicit", "artist_oids"],
    properties: {
      track_id: {
        bsonType: "string",
        description: "track_id is required and cannot be null",
      },
      album_oid: {
        bsonType: "objectId",
        description: "album_oid is required and cannot be null",
      },
      name: {
        bsonType: "string",
        description: "name is required and cannot be null",
      },
      explicit: {
        bsonType: "bool",
        description: "explicit is required and cannot be null",
      },
      artist_oids: {
        bsonType: "array",
        description: "artists is required and must be an array",
        minItems: 1, // Ensure there is at least one element in the array
        items: {
          bsonType: "object",
          required: ["artist_id"],
          properties: {
            artist_oid: {
              bsonType: "objectId",
              description: "artist_oid is required and cannot be null",
            },
          },
        },
      },
    },
  },
};

const artists = {
  $jsonSchema: {
    bsonType: "object",
    required: ["artist_id","name"],
    properties: {
      artist_id: {
        bsonType: "string",
        description: "artist_id is required and cannot be null",
      },
      name: {
        bsonType: "string",
        description: "name is required and cannot be null",
      },
      albums: {
        bsonType: "array",
        items: {
          bsonType: "objectId",
        },
      },
    },
  },
};

const albums = {
  $jsonSchema: {
    bsonType: "object",
    required: ["album_id", "type", "artist_oids", "name", "year", "total_tracks", "track_oids", "images"],
    properties: {
      album_id: {
        bsonType: "string",
        description: "album_id is required and cannot be null",
      },
      type: {
        enum: ["album", "single", "compilation"],
        description: "type is required and must be one of the enum values",
      },
      artist_oids: {
        bsonType: "array",
        description: "at least one artist_id is required and cannot be null",
        minItems: 1,
        items: {
          bsonType: "objectId",
        }
      },
      name: {
        bsonType: "string",
        description: "name is required and cannot be null",
      },
      year: {
        bsonType: "int",
        description: "year is required and cannot be null",
      },
      total_tracks: {
        bsonType: "int",
        description: "total_tracks is required and cannot be null",
      },
      track_oids: {
        bsonType: "array",
        minItems: 1,
        items: {
          bsonType: "objectId",
        },
      },
      images: {
        bsonType: "array",
        minItems: 1,
        items: {
          bsonType: "object",
          required: ["url", "height", "width"],
          properties: {
            url: {
              bsonType: "string",
              description: "url is required and cannot be null",
            },
            height: {
              bsonType: "int",
              description: "height is required and cannot be null",
            },
            width: {
              bsonType: "int",
              description: "width is required and cannot be null",
            },
          },
        },
      },
    },
  },
};

const features = {
  $jsonSchema: {
    bsonType: "object",
    required: ["track_oid", "danceability", "energy", "key", "loudness", "mode", "speechiness", "acousticness", "instrumentalness", "liveness", "valence", "tempo", "duration_ms"],
    properties: {
      track_oid: {
        bsonType: "objectId",
        description: "track_oid is required and cannot be null",
      },
      danceability: {
        bsonType: "double",
        description: "danceability is required and cannot be null",
      },
      energy: {
        bsonType: "double",
        description: "energy is required and cannot be null",
        maximum: 1
      },
      key: {
        bsonType: "int",
        description: "key is required and cannot be null",
        minimum: -1,
        maximum: 11,
      },
      loudness: {
        bsonType: "double",
        description: "loudness is required and cannot be null",
      },
      mode: {
        bsonType: "int",
        description: "mode is required and cannot be null",
        minimum: 0,
        maximum: 1
      },
      speechiness: {
        bsonType: "double",
        description: "speechiness is required and cannot be null",
      },
      acousticness: {
        bsonType: "double",
        description: "acousticness is required and cannot be null",
        minimum: 0,
        maximum: 1
      },
      instrumentalness: {
        bsonType: "double",
        description: "instrumentalness is required and cannot be null",
      },
      liveness: {
        bsonType: "double",
        description: "liveness is required and cannot be null",
      },
      valence: {
        bsonType: "double",
        description: "valence is required and cannot be null",
        minimum: 0,
        maximum: 1
      },
      tempo: {
        bsonType: "double",
        description: "tempo is required and cannot be null",
      },
      duration_ms: {
        bsonType: "int",
        description: "duration_ms is required and cannot be null",
      },
    },
  }
}

export { tracks, artists, albums, features };
