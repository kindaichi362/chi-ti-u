// Generates the Mộc Mộc dragon as GLB files (one per growth stage) with node-transform animations.
// Run: npm install && npm run build   → writes app/src/main/assets/dragon/mocmoc_stage{1..6}.glb
//
// The model is built from smooth procedural parts (no textures), so it stays tiny (~100 kB) and
// renders fast on low-end phones. Each animation is a looping clip on named pivot nodes:
// idle, happy, sleep, eat, walk, play, levelup, touch.
import { Document, NodeIO } from '@gltf-transform/core';
import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const OUT = resolve(dirname(fileURLToPath(import.meta.url)), '../../app/src/main/assets/dragon');
mkdirSync(OUT, { recursive: true });

// ---------- math ----------
const srgb = (hex) => {
  const c = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255);
  return [...c.map((v) => (v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4)), 1];
};
const quat = (x, y, z) => { // euler XYZ in degrees → quaternion [x,y,z,w]
  const [a, b, c] = [x, y, z].map((d) => (d * Math.PI) / 360);
  const [ca, sa, cb, sb, cc, sc] = [Math.cos(a), Math.sin(a), Math.cos(b), Math.sin(b), Math.cos(c), Math.sin(c)];
  return [sa * cb * cc + ca * sb * sc, ca * sb * cc - sa * cb * sc, ca * cb * sc + sa * sb * cc, ca * cb * cc - sa * sb * sc];
};

// ---------- geometry (all smooth-shaded) ----------
function ellipsoid(rx, ry, rz, seg = 28, rings = 18, yCut = -1) {
  const p = [], n = [], idx = [];
  for (let i = 0; i <= rings; i++) {
    const v = i / rings, phi = v * Math.PI;
    for (let j = 0; j <= seg; j++) {
      const u = j / seg, th = u * Math.PI * 2;
      const x = Math.sin(phi) * Math.cos(th), y = Math.cos(phi), z = Math.sin(phi) * Math.sin(th);
      p.push(x * rx, Math.max(y, yCut) * ry, z * rz);
      const nx = x / rx, ny = y / ry, nz = z / rz, l = Math.hypot(nx, ny, nz);
      n.push(nx / l, ny / l, nz / l);
    }
  }
  for (let i = 0; i < rings; i++) for (let j = 0; j < seg; j++) {
    const a = i * (seg + 1) + j, b = a + seg + 1;
    idx.push(a, b, a + 1, b, b + 1, a + 1);
  }
  return { p, n, idx };
}

// Surface of revolution around +Y from a profile of [radius, y] points (bottom → top).
function lathe(profile, seg = 20) {
  const p = [], idx = [];
  profile.forEach(([r, y]) => { for (let j = 0; j <= seg; j++) { const t = (j / seg) * Math.PI * 2; p.push(Math.cos(t) * r, y, Math.sin(t) * r); } });
  for (let i = 0; i < profile.length - 1; i++) for (let j = 0; j < seg; j++) {
    const a = i * (seg + 1) + j, b = a + seg + 1;
    idx.push(a, a + 1, b, b, a + 1, b + 1);
  }
  return { p, n: normalsOf(p, idx), idx };
}

function normalsOf(p, idx) {
  const n = new Array(p.length).fill(0);
  for (let i = 0; i < idx.length; i += 3) {
    const [a, b, c] = [idx[i] * 3, idx[i + 1] * 3, idx[i + 2] * 3];
    const ux = p[b] - p[a], uy = p[b + 1] - p[a + 1], uz = p[b + 2] - p[a + 2];
    const vx = p[c] - p[a], vy = p[c + 1] - p[a + 1], vz = p[c + 2] - p[a + 2];
    const nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
    for (const k of [a, b, c]) { n[k] += nx; n[k + 1] += ny; n[k + 2] += nz; }
  }
  for (let i = 0; i < n.length; i += 3) { const l = Math.hypot(n[i], n[i + 1], n[i + 2]) || 1; n[i] /= l; n[i + 1] /= l; n[i + 2] /= l; }
  return n;
}

// A soft bat-like wing membrane in the XY plane (root at origin, spreading toward +X), with a little thickness.
function wing(span, height) {
  const outline = [];
  const pts = 7;
  for (let i = 0; i <= pts; i++) { // top edge: gentle arc
    const t = i / pts; outline.push([t * span, height * (0.35 + 0.65 * Math.sin(t * Math.PI * 0.55))]);
  }
  for (let i = pts; i >= 0; i--) { // bottom edge: scallops
    const t = i / pts; const scallop = 0.18 * Math.abs(Math.sin(t * Math.PI * 3));
    outline.push([t * span, -height * (0.15 + scallop) * (1 - t * 0.4)]);
  }
  const p = [], idx = [], th = 0.012;
  const cx = span * 0.35, cy = height * 0.2;
  for (const z of [th, -th]) { p.push(cx, cy, z); outline.forEach(([x, y]) => p.push(x, y, z)); }
  const m = outline.length + 1;
  for (let i = 1; i < m; i++) {
    const nx = i + 1 < m ? i + 1 : 1;
    idx.push(0, i, nx);                // front
    idx.push(m, m + nx, m + i);        // back
  }
  return { p, n: normalsOf(p, idx), idx };
}

// Full ring, or an arc between a0..a1 degrees (used for the smile).
function torus(R, r, seg = 40, tube = 10, a0 = 0, a1 = 360) {
  const p = [], n = [], idx = [];
  for (let i = 0; i <= seg; i++) {
    const u = (a0 + (i / seg) * (a1 - a0)) * Math.PI / 180;
    for (let j = 0; j <= tube; j++) {
      const v = (j / tube) * Math.PI * 2;
      const cx = Math.cos(u), cz = Math.sin(u);
      p.push((R + r * Math.cos(v)) * cx, r * Math.sin(v), (R + r * Math.cos(v)) * cz);
      n.push(Math.cos(v) * cx, Math.sin(v), Math.cos(v) * cz);
    }
  }
  for (let i = 0; i < seg; i++) for (let j = 0; j < tube; j++) {
    const a = i * (tube + 1) + j, b = a + tube + 1;
    idx.push(a, b, a + 1, b, b + 1, a + 1);
  }
  return { p, n, idx };
}

// ---------- model ----------
const STAGES = [
  { name: 'so-sinh',    scale: 0.80, head: 1.18, wing: 0.50, horn: 0.40, tail: 0.75, spikes: 0, gold: false, halo: false },
  { name: 'tap-di',     scale: 0.90, head: 1.12, wing: 0.66, horn: 0.55, tail: 0.85, spikes: 0, gold: false, halo: false },
  { name: 'thieu-nien', scale: 1.00, head: 1.05, wing: 0.85, horn: 0.75, tail: 1.00, spikes: 3, gold: false, halo: false },
  { name: 'truong-thanh', scale: 1.10, head: 1.00, wing: 1.00, horn: 0.90, tail: 1.10, spikes: 4, gold: false, halo: false },
  { name: 'tinh-anh',   scale: 1.18, head: 1.00, wing: 1.15, horn: 1.00, tail: 1.15, spikes: 5, gold: true,  halo: false },
  { name: 'huyen-thoai', scale: 1.25, head: 1.00, wing: 1.30, horn: 1.15, tail: 1.20, spikes: 5, gold: true,  halo: true },
];

function build(stageIndex) {
  const S = STAGES[stageIndex];
  const doc = new Document();
  doc.createBuffer();
  const scene = doc.createScene('Scene');
  const mat = (name, hex, rough = 0.62, metal = 0, double = false) =>
    doc.createMaterial(name).setBaseColorFactor(srgb(hex)).setRoughnessFactor(rough).setMetallicFactor(metal).setDoubleSided(double);
  const M = {
    body: mat('body', '#5FC9A6'), bodyDark: mat('bodyDark', '#3FA987'), belly: mat('belly', '#FFF3D9', 0.7),
    horn: S.gold ? mat('horn', '#F2C14E', 0.3, 0.6) : mat('horn', '#F4E6C2', 0.55),
    wing: mat('wing', '#A9E8D2', 0.55, 0, true), eye: mat('eye', '#1B2621', 0.15), white: mat('white', '#FFFFFF', 0.1),
    cheek: mat('cheek', '#F7A7B4', 0.8), gold: mat('gold', '#F2C14E', 0.28, 0.75), nostril: mat('nostril', '#2F8F6F', 0.7),
  };
  const mesh = (name, g, material) => {
    const prim = doc.createPrimitive()
      .setAttribute('POSITION', doc.createAccessor().setType('VEC3').setArray(new Float32Array(g.p)))
      .setAttribute('NORMAL', doc.createAccessor().setType('VEC3').setArray(new Float32Array(g.n)))
      .setIndices(doc.createAccessor().setType('SCALAR').setArray(new Uint16Array(g.idx)))
      .setMaterial(material);
    return doc.createNode(name).setMesh(doc.createMesh(name).addPrimitive(prim));
  };
  const group = (name, t = [0, 0, 0], r = [0, 0, 0, 1], s = [1, 1, 1]) => doc.createNode(name).setTranslation(t).setRotation(r).setScale(s);

  // Root sits on the floor at y = 0.
  const root = group('Dragon', [0, 0, 0], [0, 0, 0, 1], [S.scale, S.scale, S.scale]);
  scene.addChild(root);

  // Legs (on root so the body can bob above them)
  // Short chubby legs (chibi proportions)
  const legGeo = lathe([[0.001, -0.015], [0.08, 0.0], [0.09, 0.05], [0.075, 0.12], [0.06, 0.17]]);
  const legs = {};
  for (const [nm, x, z] of [['legFL', 0.15, 0.12], ['legFR', -0.15, 0.12], ['legBL', 0.17, -0.1], ['legBR', -0.17, -0.1]]) {
    const hip = group(nm, [x, 0.17, z]);
    const leg = mesh(nm + 'Mesh', legGeo, M.bodyDark).setTranslation([0, -0.17, 0]);
    const toe = mesh(nm + 'Toe', ellipsoid(0.09, 0.04, 0.1, 16, 10), M.belly).setTranslation([0, -0.155, 0.03]);
    hip.addChild(leg).addChild(toe);
    root.addChild(hip);
    legs[nm] = hip;
  }

  // Body pivot at belly centre
  const body = group('body', [0, 0.38, 0]);
  root.addChild(body);
  body.addChild(mesh('bodyMesh', ellipsoid(0.27, 0.3, 0.25), M.body));
  body.addChild(mesh('belly', ellipsoid(0.2, 0.24, 0.12), M.belly).setTranslation([0, -0.02, 0.15]));

  // Back spikes (teen and up)
  for (let i = 0; i < S.spikes; i++) {
    const t = i / Math.max(1, S.spikes - 1);
    // Sit on the back surface of the body ellipsoid, pointing outward along its normal.
    const a = (35 + t * 55) * Math.PI / 180;
    const sp = mesh('spike' + i, lathe([[0.04, 0], [0.025, 0.05], [0.001, 0.09]], 12), S.gold ? M.gold : M.horn);
    sp.setTranslation([0, 0.29 * Math.cos(a), -0.24 * Math.sin(a)]).setRotation(quat(-(35 + t * 55), 0, 0));
    body.addChild(sp);
  }

  // Head
  const H = S.head;
  const head = group('head', [0, 0.32, 0.04], [0, 0, 0, 1], [H, H, H]);
  body.addChild(head);
  head.addChild(mesh('headMesh', ellipsoid(0.27, 0.23, 0.24), M.body).setTranslation([0, 0.17, 0]));
  head.addChild(mesh('snout', ellipsoid(0.15, 0.085, 0.1), M.belly).setTranslation([0, 0.085, 0.2]));
  for (const x of [0.04, -0.04]) head.addChild(mesh('nostril', ellipsoid(0.016, 0.011, 0.01, 10, 8), M.nostril).setTranslation([x, 0.11, 0.297]));
  for (const [nm, x] of [['eyeL', 0.105], ['eyeR', -0.105]]) {
    const eye = group(nm, [x, 0.225, 0.195]);
    eye.addChild(mesh(nm + 'Ball', ellipsoid(0.058, 0.07, 0.04), M.eye));
    eye.addChild(mesh(nm + 'Shine', ellipsoid(0.02, 0.022, 0.012, 12, 8), M.white).setTranslation([x > 0 ? 0.018 : -0.018 + 0.03, 0.025, 0.032]));
    eye.addChild(mesh(nm + 'Shine2', ellipsoid(0.009, 0.009, 0.008, 10, 6), M.white).setTranslation([x > 0 ? -0.02 : 0.0, -0.02, 0.035]));
    head.addChild(eye);
  }
  for (const x of [0.17, -0.17]) head.addChild(mesh('cheek', ellipsoid(0.045, 0.025, 0.02, 14, 8), M.cheek).setTranslation([x, 0.12, 0.17]));
  // Horns start just under the skull surface and stick well out of it.
  const hornGeo = lathe([[0.045, 0], [0.04, 0.06], [0.03, 0.12], [0.016, 0.18], [0.001, 0.22]], 14);
  for (const [nm, x, rz] of [['hornL', 0.1, -22], ['hornR', -0.1, 22]]) {
    head.addChild(mesh(nm, hornGeo, M.horn).setTranslation([x, 0.33, -0.05]).setRotation(quat(-25, 0, rz)).setScale([1, 0.55 + 0.45 * S.horn, 1]));
  }
  // Small smile under the snout
  head.addChild(mesh('mouth', torus(0.032, 0.006, 24, 6, 200, 340), M.nostril).setTranslation([0, 0.085, 0.29]).setRotation(quat(-90, 0, 0)));
  for (const x of [0.2, -0.2]) // little ear frills
    head.addChild(mesh('frill', ellipsoid(0.05, 0.03, 0.08, 14, 8), M.wing).setTranslation([x, 0.24, -0.06]).setRotation(quat(0, 0, x > 0 ? -40 : 40)));
  if (S.halo) {
    const halo = mesh('halo', torus(0.16, 0.012), M.gold).setTranslation([0, 0.55, -0.02]).setRotation(quat(12, 0, 0));
    head.addChild(halo);
  }

  // Wings (pivot at shoulder)
  const W = S.wing;
  const wingGeo = wing(0.42 * W, 0.3 * W);
  const wingL = group('wingL', [0.15, 0.15, -0.16], quat(0, -25, 18));
  const wingR = group('wingR', [-0.15, 0.15, -0.16], quat(0, 205, 18));
  wingL.addChild(mesh('wingLMesh', wingGeo, M.wing));
  wingR.addChild(mesh('wingRMesh', wingGeo, M.wing));
  for (const w of [wingL, wingR]) w.addChild(mesh('wingBone', lathe([[0.016, 0], [0.012, 0.42 * W]], 8), M.bodyDark).setRotation(quat(0, 0, -72)));
  body.addChild(wingL).addChild(wingR);

  // Tail: three chained segments, tip fin
  const T = S.tail;
  const tail = group('tail', [0, -0.12, -0.2], quat(-60, 0, 0));
  const t1 = group('tail1', [0, 0.14 * T, 0]); const t2 = group('tail2', [0, 0.12 * T, 0]);
  tail.addChild(mesh('tailSeg0', lathe([[0.12, 0], [0.1, 0.08], [0.075, 0.15 * T]], 16), M.body));
  t1.addChild(mesh('tailSeg1', lathe([[0.075, 0], [0.06, 0.07], [0.045, 0.13 * T]], 16), M.body));
  t2.addChild(mesh('tailSeg2', lathe([[0.045, 0], [0.03, 0.06], [0.012, 0.11 * T]], 14), M.body));
  t2.addChild(mesh('tailFin', ellipsoid(0.07, 0.05, 0.015, 14, 8), S.gold ? M.gold : M.wing).setTranslation([0, 0.11 * T, 0]));
  t1.addChild(t2); tail.addChild(t1); body.addChild(tail);

  addAnimations(doc, { root, body, head, wingL, wingR, tail, t1, t2, legs, eyeL: head.listChildren().find((c) => c.getName() === 'eyeL'), eyeR: head.listChildren().find((c) => c.getName() === 'eyeR'), S });
  return doc;
}

// ---------- animations ----------
function addAnimations(doc, N) {
  const buffer = doc.getRoot().listBuffers()[0];
  const base = new Map();
  for (const node of doc.getRoot().listNodes()) base.set(node, { t: node.getTranslation(), r: node.getRotation(), s: node.getScale() });

  const clip = (name, dur, tracks) => {
    const anim = doc.createAnimation(name);
    for (const [node, path, keys] of tracks) {
      const times = keys.map((k) => k[0] * dur);
      const vals = keys.flatMap((k) => k[1]);
      const sampler = doc.createAnimationSampler()
        .setInput(doc.createAccessor().setType('SCALAR').setArray(new Float32Array(times)).setBuffer(buffer))
        .setOutput(doc.createAccessor().setType(path === 'rotation' ? 'VEC4' : 'VEC3').setArray(new Float32Array(vals)).setBuffer(buffer))
        .setInterpolation('LINEAR');
      anim.addSampler(sampler).addChannel(doc.createAnimationChannel().setTargetNode(node).setTargetPath(path).setSampler(sampler));
    }
  };
  // helpers relative to bind pose
  const T = (node, ...ks) => [node, 'translation', ks.map(([t, d]) => [t, base.get(node).t.map((v, i) => v + d[i])])];
  const R = (node, ...ks) => [node, 'rotation', ks.map(([t, e]) => [t, mul(base.get(node).r, quat(...e))])];
  const S = (node, ...ks) => [node, 'scale', ks.map(([t, d]) => [t, base.get(node).s.map((v, i) => v * d[i])])];
  const steps = (n, f) => Array.from({ length: n + 1 }, (_, i) => f(i / n, i));
  const { root, body, head, wingL, wingR, tail, t1, t2, legs, eyeL, eyeR } = N;
  const flap = (deg, n) => [R(wingL, ...steps(n, (t) => [t, [0, 0, -Math.sin(t * Math.PI * 2 * (n / 4)) * deg]])), R(wingR, ...steps(n, (t) => [t, [0, 0, Math.sin(t * Math.PI * 2 * (n / 4)) * deg]]))];
  const sway = (deg, n = 8) => [R(tail, ...steps(n, (t) => [t, [0, Math.sin(t * Math.PI * 2) * deg, 0]])), R(t1, ...steps(n, (t) => [t, [0, Math.sin(t * Math.PI * 2 - 0.8) * deg, 0]])), R(t2, ...steps(n, (t) => [t, [0, Math.sin(t * Math.PI * 2 - 1.6) * deg, 0]]))];
  const blink = (at) => [S(eyeL, [0, [1, 1, 1]], [at, [1, 1, 1]], [at + 0.03, [1, 0.1, 1]], [at + 0.06, [1, 1, 1]], [1, [1, 1, 1]]), S(eyeR, [0, [1, 1, 1]], [at, [1, 1, 1]], [at + 0.03, [1, 0.1, 1]], [at + 0.06, [1, 1, 1]], [1, [1, 1, 1]])];

  clip('idle', 3.2, [
    S(body, ...steps(8, (t) => [t, [1 + 0.02 * Math.sin(t * Math.PI * 2), 1 + 0.035 * Math.sin(t * Math.PI * 2), 1 + 0.02 * Math.sin(t * Math.PI * 2)]])),
    R(head, ...steps(8, (t) => [t, [2 * Math.sin(t * Math.PI * 2), 4 * Math.sin(t * Math.PI * 2 + 1), 0]])),
    ...flap(6, 4), ...sway(10), ...blink(0.7),
  ]);
  clip('happy', 1.1, [
    T(root, [0, [0, 0, 0]], [0.15, [0, -0.03, 0]], [0.45, [0, 0.32, 0]], [0.75, [0, 0, 0]], [0.85, [0, -0.02, 0]], [1, [0, 0, 0]]),
    S(body, [0, [1, 1, 1]], [0.15, [1.08, 0.9, 1.08]], [0.45, [0.95, 1.08, 0.95]], [0.8, [1.06, 0.93, 1.06]], [1, [1, 1, 1]]),
    R(head, [0, [0, 0, 0]], [0.45, [-12, 0, 0]], [1, [0, 0, 0]]),
    ...flap(35, 8), ...sway(25),
  ]);
  clip('sleep', 4.0, [
    T(body, [0, [0, -0.12, 0]], [0.5, [0, -0.11, 0]], [1, [0, -0.12, 0]]),
    S(body, ...steps(8, (t) => [t, [1.06 + 0.02 * Math.sin(t * Math.PI * 2), 0.9 + 0.03 * Math.sin(t * Math.PI * 2), 1.06]])),
    R(head, [0, [22, 0, 8]], [0.5, [24, 0, 8]], [1, [22, 0, 8]]),
    S(eyeL, [0, [1, 0.08, 1]], [1, [1, 0.08, 1]]), S(eyeR, [0, [1, 0.08, 1]], [1, [1, 0.08, 1]]),
    R(wingL, [0, [0, 0, 25]], [1, [0, 0, 25]]), R(wingR, [0, [0, 0, -25]], [1, [0, 0, -25]]),
    R(tail, [0, [25, 40, 0]], [1, [25, 40, 0]]),
    ...Object.values(legs).map((l) => T(l, [0, [0, -0.06, 0]], [1, [0, -0.06, 0]])),
  ]);
  clip('eat', 1.6, [
    R(head, [0, [0, 0, 0]], [0.2, [28, 0, 0]], [0.3, [20, 0, 0]], [0.4, [28, 0, 0]], [0.5, [20, 0, 0]], [0.6, [28, 0, 0]], [0.8, [-6, 0, 0]], [1, [0, 0, 0]]),
    S(head, [0, [1, 1, 1]], [0.25, [1.04, 0.96, 1]], [0.35, [0.97, 1.03, 1]], [0.45, [1.04, 0.96, 1]], [0.55, [0.97, 1.03, 1]], [1, [1, 1, 1]]),
    ...sway(12), ...flap(8, 4),
  ]);
  clip('walk', 1.0, [
    T(root, ...steps(8, (t) => [t, [0, Math.abs(Math.sin(t * Math.PI * 2)) * 0.035, 0]])),
    R(body, ...steps(8, (t) => [t, [0, 0, Math.sin(t * Math.PI * 2) * 5]])),
    R(legs.legFL, ...steps(8, (t) => [t, [Math.sin(t * Math.PI * 2) * 28, 0, 0]])),
    R(legs.legBR, ...steps(8, (t) => [t, [Math.sin(t * Math.PI * 2) * 28, 0, 0]])),
    R(legs.legFR, ...steps(8, (t) => [t, [-Math.sin(t * Math.PI * 2) * 28, 0, 0]])),
    R(legs.legBL, ...steps(8, (t) => [t, [-Math.sin(t * Math.PI * 2) * 28, 0, 0]])),
    R(head, ...steps(8, (t) => [t, [Math.sin(t * Math.PI * 4) * 4, 0, 0]])),
    ...sway(15), ...flap(8, 4),
  ]);
  clip('play', 1.8, [
    R(root, ...steps(12, (t) => [t, [0, t * 360, 0]])),
    T(root, [0, [0, 0, 0]], [0.25, [0, 0.18, 0]], [0.5, [0, 0, 0]], [0.75, [0, 0.18, 0]], [1, [0, 0, 0]]),
    R(head, [0, [0, 0, 0]], [0.25, [0, 0, 15]], [0.75, [0, 0, -15]], [1, [0, 0, 0]]),
    ...flap(30, 8), ...sway(30),
  ]);
  clip('levelup', 2.4, [
    T(root, [0, [0, 0, 0]], [0.12, [0, -0.05, 0]], [0.4, [0, 0.55, 0]], [0.65, [0, 0.5, 0]], [0.9, [0, 0, 0]], [1, [0, 0, 0]]),
    R(root, [0, [0, 0, 0]], [0.12, [0, 0, 0]], [0.4, [0, 360, 0]], [1, [0, 360, 0]]),
    S(root, [0, [1, 1, 1]], [0.12, [1.1, 0.88, 1.1]], [0.4, [1.12, 1.12, 1.12]], [0.9, [1, 1, 1]], [0.95, [1.08, 0.92, 1.08]], [1, [1, 1, 1]]),
    R(wingL, [0, [0, 0, 0]], [0.4, [0, 0, -45]], [0.5, [0, 0, 10]], [0.6, [0, 0, -45]], [0.7, [0, 0, 10]], [1, [0, 0, 0]]),
    R(wingR, [0, [0, 0, 0]], [0.4, [0, 0, 45]], [0.5, [0, 0, -10]], [0.6, [0, 0, 45]], [0.7, [0, 0, -10]], [1, [0, 0, 0]]),
    R(head, [0, [0, 0, 0]], [0.5, [-20, 0, 0]], [1, [0, 0, 0]]),
    ...sway(30),
  ]);
  clip('touch', 0.9, [
    S(body, [0, [1, 1, 1]], [0.12, [1.12, 0.86, 1.12]], [0.3, [0.93, 1.1, 0.93]], [0.5, [1.04, 0.97, 1.04]], [1, [1, 1, 1]]),
    R(head, [0, [0, 0, 0]], [0.2, [0, 0, 14]], [0.5, [0, 0, -8]], [1, [0, 0, 0]]),
    S(eyeL, [0, [1, 1, 1]], [0.1, [1.1, 0.3, 1]], [0.5, [1.1, 0.3, 1]], [0.7, [1, 1, 1]], [1, [1, 1, 1]]),
    S(eyeR, [0, [1, 1, 1]], [0.1, [1.1, 0.3, 1]], [0.5, [1.1, 0.3, 1]], [0.7, [1, 1, 1]], [1, [1, 1, 1]]),
    ...flap(20, 4),
  ]);
}

function mul(a, b) { // quaternion a*b
  const [ax, ay, az, aw] = a, [bx, by, bz, bw] = b;
  return [aw * bx + ax * bw + ay * bz - az * by, aw * by - ax * bz + ay * bw + az * bx, aw * bz + ax * by - ay * bx + az * bw, aw * bw - ax * bx - ay * by - az * bz];
}

const io = new NodeIO();
for (let i = 0; i < STAGES.length; i++) {
  const file = `${OUT}/mocmoc_stage${i + 1}.glb`;
  await io.write(file, build(i));
  console.log('wrote', file);
}
