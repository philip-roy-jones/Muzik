import {createServer} from "https";
import {parse} from "url";
import next from "next";
import fs from "fs";
import path from "path";

const dev = process.env.NODE_ENV !== "production";
const app = next({dev});
const handle = app.getRequestHandler();

const httpsOptions = {
  key: fs.readFileSync(path.join(__dirname, "key.pem")),
  cert: fs.readFileSync(path.join(__dirname, "localhost.pem")),
};

app.prepare().then(() => {
  try {
    const server = createServer(httpsOptions, (req, res) => {
      const parsedUrl = parse(req.url || "", true);
      handle(req, res, parsedUrl);
    });

    server.listen(3000, () => {
      console.log("> Server started on https://localhost:3000");
    });
  } catch (error) {
    console.error("Failed to start server:", error);
  }
});