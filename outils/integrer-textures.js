// Intègre les textures peintes du dossier textures/ dans docs/fenua.html, pour que le jeu reste
// un seul fichier autonome (il marche aussi ouvert directement depuis le disque).
// Usage : node outils/integrer-textures.js
//
// Chaque fichier textures/<nom>.webp (ou .jpg, .png) devient une ligne du bloc
// <script type="application/json" id="textures-peintes">, entre les deux repères ci-dessous.
// Le jeu y cherche les noms de PEINTES (section 1) ; un nom absent garde sa texture dessinée.
// Pour changer une texture : remplacer le fichier (256 × 256, qui se raccorde sur ses bords),
// relancer ce script, puis node outils/verifier-script.js.
const fs=require("fs"), path=require("path");
const racine=path.join(__dirname,".."), dossier=path.join(racine,"textures"), jeu=path.join(racine,"docs","fenua.html");
const TYPES={".webp":"image/webp",".jpg":"image/jpeg",".jpeg":"image/jpeg",".png":"image/png"};
const DEBUT="<!-- textures peintes : générées par outils/integrer-textures.js, ne pas modifier à la main -->";
const FIN="<!-- fin des textures peintes -->";

const fichiers=fs.readdirSync(dossier).filter(f=>TYPES[path.extname(f).toLowerCase()]&&!f.startsWith("grille")).sort();
const lignes=fichiers.map(f=>{
  const nom=path.basename(f,path.extname(f)), b64=fs.readFileSync(path.join(dossier,f)).toString("base64");
  return JSON.stringify(nom)+":"+JSON.stringify("data:"+TYPES[path.extname(f).toLowerCase()]+";base64,"+b64);
});
const bloc=DEBUT+'\n<script type="application/json" id="textures-peintes">{\n'+lignes.join(",\n")+'\n}</script>\n'+FIN;

let h=fs.readFileSync(jeu,"utf8");
const a=h.indexOf(DEBUT), b=h.indexOf(FIN);
if(a>=0&&b>a) h=h.slice(0,a)+bloc+h.slice(b+FIN.length);
else{ // première fois : juste avant le script du jeu
  const i=h.lastIndexOf("<script>"); if(i<0){ console.error("Script du jeu introuvable."); process.exit(1); }
  h=h.slice(0,i)+bloc+"\n"+h.slice(i);
}
fs.writeFileSync(jeu,h);
console.log(fichiers.length+" textures intégrées ("+fichiers.map(f=>path.basename(f,path.extname(f))).join(", ")+") · "+
  Math.round(h.length/1024)+" Ko");
