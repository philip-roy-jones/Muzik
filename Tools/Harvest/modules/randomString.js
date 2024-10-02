import { promises as fs } from "fs";

async function readJsonFile(filePath) {
  try {
    const data = await fs.readFile(filePath, "utf8");
    const jsonData = JSON.parse(data);
    return jsonData;
  } catch (err) {
    console.error("Error reading or parsing the file:", err);
    return null;
  }
}

function removeLeadingTrailingWhitespace(str) {
  let left = 0;
  let leadingWhitespace = true;
  while (leadingWhitespace && left < str.length) {
    if (str.slice(left, left + 3) === "%20") {
      left += 3;
    } else if (str.slice(left, left + 6) === "%C2%A0") {
      left += 6;
    } else {
      leadingWhitespace = false;
      str = str.slice(left);
    }
  }

  if (left >= str.length) {
    return "";
  }

  let right = str.length - 1;
  let trailingWhitespace = true;
  while (trailingWhitespace && right >= 0) {
    if (str.slice(right - 2, right + 1) === "%20") {
      right -= 3;
    } else if (str.slice(right - 5, right + 1) === "%C2%A0") {
      right -= 6;
    } else {
      trailingWhitespace = false;
      str = str.slice(0, right + 1);
    }
  }

  return str;
}

async function randomString(minLength = 1, maxLength = 15) {
  const utf8EncodingPath = "./utf8PercentEncodingData.json"; // From perspective of main.js
  const encodings = await readJsonFile(utf8EncodingPath);

  if (!encodings) {
    console.error("Error reading the encoding data");
    return null;
  }

  // Generate a random length between minLength and maxLength
  const length =
    Math.floor(Math.random() * (maxLength - minLength + 1)) + minLength;

  // Generate a string of that length using the allowed characters
  let result = "";
  for (let i = 0; i < length; i++) {
    result += encodings[Math.floor(Math.random() * encodings.length)];
  }

  return removeLeadingTrailingWhitespace(result);
}

// let test = "%C2%A0";

// console.log(removeLeadingTrailingWhitespace(test));

export default randomString;
