import axios from "axios";

const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL;

async function verifySpotifyConnection(): Promise<boolean> {
  try {
    const response = await axios.get(`${backendUrl}/api/v1/spotify/verify`, {withCredentials: true});
    return response.data["connected?"] === true;
  } catch (error) {
    console.error('Error verifying Spotify connection:', error);
    return false;
  }
}

async function fetchSpotifyAuthUrl() {
  try {
    const response = await axios.get(`${backendUrl}/api/v1/spotify/authorize`, { withCredentials: true });
    if (!response.data.authorizationUrl) {
      throw new Error('Authorization URL not found in the response');
    }
    return response.data.authorizationUrl;
  } catch (error) {
    console.error('Error fetching Spotify authorization URL:', error);
    throw error;
  }
}

export {verifySpotifyConnection, fetchSpotifyAuthUrl};