// Cuts the dragon artwork out of the character sheet (docs/dragon-home/character-sheet.webp) into
// app drawables: 6 growth stages, 7 action poses (transparent background) and 8 face tiles.
// Run: npm install && npm run build
//
// Background removal: flood fill from the crop border through pixels that change only gradually
// (the sheet's grey gradient and soft floor shadow) and stay close to the border colour; then keep
// the largest connected shape, so bits of neighbouring dragons that enter the crop are dropped.
// Upscaling: Lanczos 3 + unsharp mask. This sharpens edges but cannot invent detail the
// 1536×1024 sheet does not have.
import sharp from 'sharp';
import { mkdirSync } from 'node:fs';

const SHEET = '../../docs/dragon-home/character-sheet.webp';
const RES = '../../app/src/main/res/drawable-nodpi';
mkdirSync(RES, { recursive: true });

// [name, left, top, width, height] in sheet pixels
const FIGURES = [
  ['dragon_stage1', 6, 848, 94, 104],
  ['dragon_stage2', 108, 823, 102, 129],
  ['dragon_stage3', 224, 791, 113, 161],
  ['dragon_stage4', 342, 750, 150, 203],
  ['dragon_stage5', 504, 735, 168, 218],
  ['dragon_stage6', 669, 708, 200, 245],
  ['dragon_pose_walk', 152, 458, 114, 158],
  ['dragon_pose_run', 266, 461, 118, 155],
  ['dragon_pose_happy', 387, 418, 134, 166],
  ['dragon_pose_eat', 506, 476, 139, 145, 40],
  ['dragon_pose_play', 646, 446, 150, 170],
  ['dragon_pose_sleep', 795, 516, 138, 103],
];
const FACES = [
  ['dragon_face_normal', 989, 49], ['dragon_face_happy', 1127, 49], ['dragon_face_laugh', 1260, 49], ['dragon_face_wink', 1396, 49],
  ['dragon_face_surprised', 989, 212], ['dragon_face_sad', 1127, 212], ['dragon_face_angry', 1260, 212], ['dragon_face_cute', 1396, 212],
];
const TARGET_H = 640;

function cutout(data, w, h, near = 60, pocketMin = 25) {
  const px = (i) => [data[i * 4], data[i * 4 + 1], data[i * 4 + 2]];
  const dist = (a, b) => Math.hypot(a[0] - b[0], a[1] - b[1], a[2] - b[2]);
  const border = [];
  for (let x = 0; x < w; x++) border.push(x, (h - 1) * w + x);
  for (let y = 0; y < h; y++) border.push(y * w, y * w + w - 1);
  const mean = [0, 1, 2].map((c) => border.reduce((s, i) => s + data[i * 4 + c], 0) / border.length);

  const bg = new Uint8Array(w * h);
  const queue = [], seed = new Int32Array(w * h);  // each background pixel remembers the border pixel it came from
  for (const i of border) if (dist(px(i), mean) < near) { bg[i] = 1; seed[i] = i; queue.push(i); }
  while (queue.length) {
    const i = queue.pop(), x = i % w, y = (i / w) | 0, c = px(i);
    for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
      const nx = x + dx, ny = y + dy;
      if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
      const j = ny * w + nx;
      if (bg[j]) continue;
      const cj = px(j);
      if (dist(cj, c) < 9 && dist(cj, px(seed[i])) < near) { bg[j] = 1; seed[j] = seed[i]; queue.push(j); }
    }
  }
  // Background pockets enclosed by the dragon (e.g. between tail and wing): close to the
  // background colour and big enough not to be a highlight.
  const seen = new Uint8Array(w * h);
  for (let s0 = 0; s0 < w * h; s0++) {
    if (bg[s0] || seen[s0] || dist(px(s0), mean) > 28) continue;
    const region = [s0], st = [s0]; seen[s0] = 1;
    while (st.length) {
      const i = st.pop(), x = i % w, y = (i / w) | 0;
      for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
        const nx = x + dx, ny = y + dy;
        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
        const j = ny * w + nx;
        if (!bg[j] && !seen[j] && dist(px(j), mean) <= 28) { seen[j] = 1; st.push(j); region.push(j); }
      }
    }
    if (region.length > pocketMin) for (const i of region) bg[i] = 1;
  }
  // Drop the 1px rim that still carries the grey background blended in.
  const rim = [];
  for (let i = 0; i < w * h; i++) {
    if (bg[i]) continue;
    const x = i % w, y = (i / w) | 0;
    if ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => { const nx = x + dx, ny = y + dy; return nx < 0 || ny < 0 || nx >= w || ny >= h || bg[ny * w + nx]; })) rim.push(i);
  }
  for (const i of rim) bg[i] = 1;
  // Keep the largest 8-connected foreground shape.
  const label = new Int32Array(w * h).fill(-1);
  let best = -1, bestSize = 0, id = 0;
  for (let s = 0; s < w * h; s++) {
    if (bg[s] || label[s] >= 0) continue;
    let size = 0; const st = [s]; label[s] = id;
    while (st.length) {
      const i = st.pop(); size++;
      const x = i % w, y = (i / w) | 0;
      for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) {
        const nx = x + dx, ny = y + dy;
        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
        const j = ny * w + nx;
        if (!bg[j] && label[j] < 0) { label[j] = id; st.push(j); }
      }
    }
    if (size > bestSize) { bestSize = size; best = id; }
    id++;
  }
  // Binary mask, and colour "bleeding": transparent pixels take the colour of the nearest kept
  // pixel, so upscaling never mixes the grey background into the dragon's edge.
  const mask = Buffer.alloc(w * h);
  let minX = w, minY = h, maxX = 0, maxY = 0;
  for (let i = 0; i < w * h; i++) {
    if (label[i] !== best) continue;
    mask[i] = 255;
    const x = i % w, y = (i / w) | 0; minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
  }
  const rgb = Buffer.alloc(w * h * 3);
  const filled = new Uint8Array(w * h);
  let frontier = [];
  for (let i = 0; i < w * h; i++) if (mask[i]) { filled[i] = 1; rgb.set([data[i * 4], data[i * 4 + 1], data[i * 4 + 2]], i * 3); frontier.push(i); }
  for (let pass = 0; pass < 12 && frontier.length; pass++) {
    const next = [];
    for (const i of frontier) {
      const x = i % w, y = (i / w) | 0;
      for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
        const nx = x + dx, ny = y + dy;
        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
        const j = ny * w + nx;
        if (filled[j]) continue;
        filled[j] = 1; rgb.copy(rgb, j * 3, i * 3, i * 3 + 3); next.push(j);
      }
    }
    frontier = next;
  }
  return { rgb, mask, box: { left: minX, top: minY, width: maxX - minX + 1, height: maxY - minY + 1 } };
}

// Upscale in 2× steps (Lanczos), sharpening a little each time; the outline comes from a blurred,
// re-thresholded mask so it is smooth instead of a magnified staircase.
async function upscale(rgb, mask, w, h, box, targetH) {
  const pad = 4;
  const ext = { top: pad, bottom: pad, left: pad, right: pad };
  let img = sharp(rgb, { raw: { width: w, height: h, channels: 3 } }).extract(box).extend({ ...ext, extendWith: 'copy' });
  let cur = await img.raw().toBuffer({ resolveWithObject: true });
  while (cur.info.height * 2 <= targetH) {
    cur = await sharp(cur.data, { raw: cur.info }).resize(cur.info.width * 2, cur.info.height * 2, { kernel: 'lanczos3' })
      .sharpen({ sigma: 0.8, m1: 0.4, m2: 1.2 }).raw().toBuffer({ resolveWithObject: true });
  }
  const W = Math.round(cur.info.width * targetH / cur.info.height);
  const color = await sharp(cur.data, { raw: cur.info }).resize(W, targetH, { kernel: 'lanczos3', fit: 'fill' }).sharpen({ sigma: 1.0, m1: 0.5, m2: 1.6 }).removeAlpha().raw().toBuffer({ resolveWithObject: true });
  const k = targetH / (box.height + pad * 2);
  // sharp applies extend after resize, so pad the mask in its own pass first.
  const padded = await sharp(mask, { raw: { width: w, height: h, channels: 1 } }).extract(box)
    .extend({ ...ext, background: { r: 0, g: 0, b: 0, alpha: 1 } }).extractChannel(0).raw().toBuffer({ resolveWithObject: true });
  const alpha = await sharp(padded.data, { raw: padded.info }).resize(W, targetH, { kernel: 'cubic', fit: 'fill' })
    .blur(Math.max(0.6, k * 0.45)).linear(2.6, 128 - 2.6 * 140).extractChannel(0).raw().toBuffer();
  if (color.info.channels !== 3 || alpha.length !== W * targetH) throw new Error(`buffer mismatch ${color.info.channels} ${alpha.length} ${W * targetH}`);
  return sharp(color.data, { raw: color.info }).joinChannel(alpha, { raw: { width: W, height: targetH, channels: 1 } });
}

for (const [name, left, top, width, height, near] of FIGURES) {
  const { data, info } = await sharp(SHEET).extract({ left, top, width, height }).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  const { rgb, mask, box } = cutout(data, info.width, info.height, near);
  const out = await upscale(rgb, mask, info.width, info.height, box, TARGET_H);
  await out.webp({ quality: 92, alphaQuality: 100 }).toFile(`${RES}/${name}.webp`);
  console.log(name, `${box.width}x${box.height} → ${TARGET_H}px tall`);
}

for (const [name, left, top] of FACES) {
  await sharp(SHEET).extract({ left, top, width: 120, height: 120 })
    .resize(256, 256, { kernel: 'lanczos3' }).sharpen({ sigma: 1.0, m1: 0.6, m2: 2.0 })
    .webp({ quality: 90 }).toFile(`${RES}/${name}.webp`);
  console.log(name);
}

// High-resolution turnaround of the young dragon (shared by the user, 1254×1254, white background):
// used as the main picture for the first stages. Only downscaled, so it stays sharp.
const REFS = [
  ['dragon_ref_front', 'src/ref1.png'],  // ref2 (back) and ref3 (side) are kept in src/ for later use
  ['dragon_ref_left', 'src/ref4.png'], ['dragon_ref_right', 'src/ref5.png'],
];
for (const [name, file] of REFS) {
  const { data, info } = await sharp(file).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  // White background: tight colour match, and only big enclosed white areas count as background
  // (gaps between arm and body), never the eye highlights.
  const { rgb, mask, box } = cutout(data, info.width, info.height, 40, 4000);
  const H = Math.min(900, box.height);
  const color = await sharp(rgb, { raw: { width: info.width, height: info.height, channels: 3 } }).extract(box).resize({ height: H, kernel: 'lanczos3' }).raw().toBuffer({ resolveWithObject: true });
  const alpha = await sharp(mask, { raw: { width: info.width, height: info.height, channels: 1 } }).extract(box).blur(1.2).linear(1.8, 128 - 1.8 * 140).resize({ height: H }).extractChannel(0).raw().toBuffer();
  await sharp(color.data, { raw: color.info }).joinChannel(alpha, { raw: { width: color.info.width, height: H, channels: 1 } })
    .webp({ quality: 90, alphaQuality: 100 }).toFile(`${RES}/${name}.webp`);
  console.log(name, `${box.width}x${box.height} → ${H}px tall`);
}
