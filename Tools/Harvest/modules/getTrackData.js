async function getTrackData() {
    try {
      const response = await axios.get('https://api.spotify.com/v1/search', {
        headers: {
          'Authorization': `Bearer ${SPOTIFY_ACCESS_TOKEN}`
        }
      });
      const tracks = response.data.items;
      // Go through each track and check if it's in the database
      // If it's not in the database, add it
    } catch (error) {
      console.error('Error fetching data from Spotify API', error);
    }
  };

export default getTrackData;