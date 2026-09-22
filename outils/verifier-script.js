// Vérifie que le <script> du jeu est syntaxiquement correct, et surtout que le
// crochet de test `window.T` n'a pas été livré par mégarde.
// Usage : node outils/verifier-script.js
const fs=require("fs"), path=require("path");
const f=path.join(__dirname,"..","docs","fenua.html");
const h=fs.readFileSync(f,"utf8");

const a=h.lastIndexOf("<script>"), b=h.lastIndexOf("</script>");
if(a<0||b<a){ console.error("Aucun bloc <script> trouvé dans docs/fenua.html."); process.exit(1); }

try{ new Function(h.slice(a+8,b)); }
catch(e){ console.error("Erreur de syntaxe dans le script du jeu : "+e.message); process.exit(1); }

if(/window\.T\s*=/.test(h)){
  console.error("Le crochet de test « window.T » est encore dans docs/fenua.html. On ne le livre jamais.");
  process.exit(1);
}

// Seules sources externes autorisées pour la publication.
const OK=[/^https:\/\/cdnjs\.cloudflare\.com(\/|$)/,/^https:\/\/cdn\.jsdelivr\.net\/npm(\/|$)/,
          /^https:\/\/fonts\.googleapis\.com(\/|$)/,/^https:\/\/fonts\.gstatic\.com(\/|$)/];
const externes=[...h.matchAll(/https:\/\/[^"'\s)]+/g)].map(m=>m[0])
  .filter(u=>!u.startsWith("https://www.w3.org/"));
const interdites=[...new Set(externes.filter(u=>!OK.some(r=>r.test(u))))];
if(interdites.length){
  console.error("Sources externes non autorisées :\n  "+interdites.join("\n  "));
  process.exit(1);
}

console.log("Syntaxe JS : correcte · crochet de test : absent · sources externes : "+
  [...new Set(externes.map(u=>u.split("/")[2]))].join(", ")+
  " · "+Math.round(h.length/1024)+" Ko");
