import * as validator from './modules/validator.js';
import { MongoClient } from 'mongodb';
import { ensureCollection } from './modules/ensure-collection.js';
import dotenv from 'dotenv';

dotenv.config({ path: '../../.env' });

async function run() {
  const uri = process.env.DATABASE_CONNECTION_STRING;
  const client = new MongoClient(uri);
  console.log(uri)
  try {
    await client.connect();
    const databaseName = 'muzik';
    const database = client.db(databaseName);

    await ensureCollection(database, 'tracks', validator.tracks);
    await ensureCollection(database, 'artists', validator.artists);
    await ensureCollection(database, 'albums', validator.albums);
    await ensureCollection(database, 'features', validator.features);

  } finally {
    await client.close();
  }
}

run().catch(console.dir);