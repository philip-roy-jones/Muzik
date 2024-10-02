import randomString from "./modules/randomString.js";
import dotenv from "dotenv";
import axios from "axios";

dotenv.config({ path: '../../.env' });

const SPOTIFY_ACCESS_TOKEN = process.env.SPOTIFY_ACCESS_TOKEN;

// Make request to Spotify API

console.log(await randomString(1,15))

// Go through each track and check if it's in the database

// If it's not in the database, add it