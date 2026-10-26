// node r.cjs in.svg out.png  — rasterize an SVG at its own size (Kalam/Inter available)
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
(async () => {
  const [inp, out] = process.argv.slice(2);
  const svg = fs.readFileSync(inp, 'utf8');
  const m = svg.match(/width="(\d+)" height="(\d+)"/);
  const F = '/home/user/Srutam/promo-video/public/fonts';
  const html = path.resolve(inp + '.html');
  fs.writeFileSync(html, `<html><head><style>
@font-face{font-family:Kalam;src:url(file://${F}/Kalam-Bold.woff2);font-weight:700}
@font-face{font-family:Kalam;src:url(file://${F}/Kalam-Regular.woff2);font-weight:400}
@font-face{font-family:Inter;src:url(file://${F}/Inter-Bold.otf);font-weight:700}
body{margin:0}</style></head><body>${svg}</body></html>`);
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: +m[1], height: +m[2] } });
  await p.goto('file://' + html);
  await p.evaluate(() => document.fonts.ready);
  await p.screenshot({ path: out });
  await b.close();
})();
