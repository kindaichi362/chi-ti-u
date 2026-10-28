// Tiny static server for preview.html: http://localhost:8787/?stage=1&clip=idle
// POST /save?name=dragon_stage1 with a PNG data URL body writes app/src/main/res/drawable-nodpi/<name>.png
import { createServer } from 'node:http';
import { readFile, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const main = resolve(here, '../../app/src/main');
createServer(async (req, res) => {
  const url = new URL(req.url, 'http://x');
  try {
    if (req.method === 'POST' && url.pathname === '/save') {
      const name = (url.searchParams.get('name') || '').replace(/[^a-z0-9_]/g, '');
      let body = ''; for await (const c of req) body += c;
      const png = Buffer.from(body.replace(/^data:image\/png;base64,/, ''), 'base64');
      await writeFile(join(main, 'res/drawable-nodpi', name + '.png'), png);
      res.writeHead(200).end('saved ' + name);
    } else if (url.pathname.startsWith('/assets/')) {
      res.writeHead(200, { 'Content-Type': 'model/gltf-binary' }).end(await readFile(join(main, url.pathname)));
    } else {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' }).end(await readFile(join(here, 'preview.html')));
    }
  } catch (e) { res.writeHead(404).end(String(e)); }
}).listen(8787, '127.0.0.1', () => console.log('preview on http://localhost:8787'));
