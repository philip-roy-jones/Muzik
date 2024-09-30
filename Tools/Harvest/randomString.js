function randomString(minLength = 1, maxLength = 15) {
    // Create an array of ASCII characters from 32 to 126, excluding uppercase (65-90)
    const asciiChars = [];
    for (let i = 32; i < 127; i++) {
        if (i < 65 || i > 90) {
            asciiChars.push(String.fromCharCode(i));
        }
    }

    // Generate a random length between minLength and maxLength
    const length = Math.floor(Math.random() * (maxLength - minLength + 1)) + minLength;

    // Generate a string of that length using the allowed characters
    let result = '';
    for (let i = 0; i < length; i++) {
        result += asciiChars[Math.floor(Math.random() * asciiChars.length)];
    }

    return result.trim();
}

export default randomString;