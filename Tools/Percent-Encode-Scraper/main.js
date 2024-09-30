const axios = require('axios');
const cheerio = require('cheerio');
const fs = require('fs');

async function scrapeTable(url) {
    try {
        // Fetch the HTML content of the website
        const { data } = await axios.get(url);
        
        // Load the HTML content into cheerio
        const $ = cheerio.load(data);
        
        // Select the table (adjust the selector as needed)
        const table = $('.ws-table-all').first(); 
        
        // Array to store the table data
        const tableData = [];
        
        // Iterate over the table rows
        table.find('tr').each((index, element) => {
            const row = $(element);
            
            // Iterate over the table cells
            row.find('td').each((i, cell) => {
                if (i === 2) {
                    tableData.push($(cell).text().trim());
                }
            });
            
        });
        
        // Write the table data to a JSON file
        fs.writeFileSync('utf8PercentEncodingData.json', JSON.stringify(tableData, null, 2));
        console.log('Table data saved to utf8PercentEncodingData.json');
    } catch (error) {
        console.error('Error fetching the website:', error);
    }
}


const url = 'https://www.w3schools.com/tags/ref_urlencode.ASP';
scrapeTable(url);