// Contact sheet of the cut-outs on a checkerboard, to check edges.
import sharp from 'sharp';
import { readdirSync } from 'node:fs';
const RES = '../../app/src/main/res/drawable-nodpi';
const files = readdirSync(RES).filter((f) => f.startsWith(process.argv[2] || 'dragon_')).sort();
const H = 320, tiles = [];
for (const f of files) { const b = await sharp(`${RES}/${f}`).resize({ height: H }).png().toBuffer(); const m = await sharp(b).metadata(); tiles.push({ b, w: m.width }); }
const W = tiles.reduce((s, t) => s + t.w + 10, 0);
const sq = 16; let checker = '';
for (let y = 0; y < H; y += sq) for (let x = 0; x < W; x += sq) if (((x + y) / sq) % 2 === 0) checker += `<rect x="${x}" y="${y}" width="${sq}" height="${sq}" fill="#ddd"/>`;
let x = 0; const comps = [];
for (const t of tiles) { comps.push({ input: t.b, left: x, top: 0 }); x += t.w + 10; }
await sharp(Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}"><rect width="100%" height="100%" fill="#fff"/>${checker}</svg>`)).composite(comps).png().toFile(`out/${process.argv[3] || 'contact'}.png`);
