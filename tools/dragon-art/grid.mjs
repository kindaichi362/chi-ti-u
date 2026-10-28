// Debug helper: crop a region of the sheet and draw a 25px grid with labels so crop boxes can be read off.
import sharp from 'sharp';
const [x, y, w, h, name] = process.argv.slice(2);
const X = +x, Y = +y, W = +w, H = +h, k = 2;
let lines = '';
for (let gx = Math.ceil(X / 25) * 25; gx < X + W; gx += 25) lines += `<line x1="${(gx - X) * k}" y1="0" x2="${(gx - X) * k}" y2="${H * k}" stroke="${gx % 100 ? '#f0f' : '#f00'}" stroke-opacity="${gx % 100 ? 0.25 : 0.7}"/>` + (gx % 100 ? '' : `<text x="${(gx - X) * k + 2}" y="14" font-size="14" fill="#f00">${gx}</text>`);
for (let gy = Math.ceil(Y / 25) * 25; gy < Y + H; gy += 25) lines += `<line x1="0" y1="${(gy - Y) * k}" x2="${W * k}" y2="${(gy - Y) * k}" stroke="${gy % 100 ? '#0af' : '#00f'}" stroke-opacity="${gy % 100 ? 0.25 : 0.7}"/>` + (gy % 100 ? '' : `<text x="2" y="${(gy - Y) * k - 2}" font-size="14" fill="#00f">${gy}</text>`);
const svg = Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${W * k}" height="${H * k}">${lines}</svg>`);
await sharp('../../docs/dragon-home/character-sheet.webp').extract({ left: X, top: Y, width: W, height: H }).resize(W * k, H * k).composite([{ input: svg }]).png().toFile(`out/${name}.png`);
