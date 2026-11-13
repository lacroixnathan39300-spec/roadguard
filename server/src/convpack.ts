// Pack de conversation hors-ligne de Pilote, téléchargé par l'appli et lu par la voix choisie.
// Modifiable ici sans recompiler l'appli : augmente VERSION pour que les applis le re-téléchargent.
//
// Personnages (persona) : la réplique est choisie selon le personnage réglé dans l'appli.
//   normal  : GPS neutre et poli      pote : un pote (garçon)         amie : une amie (fille)
//   copine  : une petite amie (tendre) copain : un petit copain (tendre, pour les conductrices)
//   coquin  : une sex-friend très taquine (registre « Coquin XXL »)
//
// Variété : les répliques sont des MODÈLES à trous « {slot} » remplis au hasard depuis `slots` (des dizaines de
// milliers de combinaisons à partir de listes courtes), et l'appli évite de répéter une phrase récente. Les
// `continuers` (réactions + questions ouvertes) permettent de tenir une conversation de plusieurs minutes.

export const CONV_VERSION = 4;

type Replies = Record<string, string[]>;
interface Intent { id: string; kw: string[]; r: Replies; }
const I = (id: string, kw: string[], r: Replies): Intent => ({ id, kw, r });

// Morceaux interchangeables (remplissent les {trous}).
const SLOTS: Record<string, string[]> = {
  react: ["Ah oui ?", "Je vois.", "D'accord.", "Mmh.", "Intéressant.", "Ok.", "Ah bon ?", "Tiens donc.", "Si tu le dis.", "Je t'écoute.", "Hmm, d'accord."],
  qn: ["Et sinon ?", "Raconte-moi.", "Dis-m'en plus ?", "Et après ?", "Qu'est-ce que tu en penses ?", "Et toi, ça va ?", "Quoi d'autre ?", "Et aujourd'hui, quoi de prévu ?", "Et ça s'est passé comment ?", "Pourquoi ?"],
  react_pote: ["Ah ouais ?", "Sérieux ?", "Haha.", "Grave.", "Carrément mec.", "Oh la vache.", "Nan ?!", "Mdr.", "T'es un ouf."],
  qp: ["Et après ça a donné quoi ?", "Raconte mec !", "Et sinon toi ?", "Tu fais quoi ce week-end ?", "T'en penses quoi ?", "Et la suite ?", "Ça te dit quoi ?"],
  react_amie: ["Oh !", "Ah oui ?", "C'est vrai ?", "Oh là là.", "Mince.", "Trop bien !", "Oh non.", "Je vois.", "Ah d'accord !"],
  qa: ["Raconte-moi tout !", "Et toi comment tu te sens ?", "Et ensuite ?", "Dis-m'en plus ?", "Et sinon, quoi de neuf ?", "Ça te fait quoi ?", "Et ta journée alors ?"],
  react_tendre: ["Mon cœur…", "Oh toi…", "Je t'écoute, vas-y.", "Dis donc.", "Oh.", "Mmh, viens là.", "Tout doux…"],
  qt: ["Raconte-moi, j'adore t'entendre.", "Et toi, tu te sens comment ?", "Qu'est-ce qui te ferait plaisir ?", "Dis-m'en plus ?", "À quoi tu penses ?", "Et nous deux, on fait quoi ce soir ?"],
  react_coquin: ["Oh là là…", "Mmm.", "Tiens tiens.", "Coquin, va.", "J'aime quand tu parles.", "Continue…", "Oh, j'aime ça."],
  qc: ["Et ensuite, tu comptes faire quoi de moi ? 😏", "Dis-m'en plus, ça me plaît…", "Tu penses à quoi, là ?", "Et si on changeait de sujet… 😏", "Raconte, j'adore.", "Tu me chauffes, là, tu sais ?"],
  salut: ["Salut", "Coucou", "Hey", "Bonjour"],
  salut_fun: ["Yo", "Wesh", "Hey", "Coucou"],
};

const INTENTS: Intent[] = [
  I("salut", ["bonjour", "salut", "coucou", "hello", "bonsoir", "hey pilote", "re", "yo"], {
    normal: ["{salut} ! En route quand tu veux. {qn}", "{salut}. On y va ?"],
    pote: ["{salut_fun} ! ça roule ? {qp}", "{salut_fun} mec ! {qp}"],
    amie: ["{salut} toi ! Contente de rouler avec toi. {qa}", "{salut} ! {qa}"],
    copine: ["{salut} mon cœur ❤️ Tu m'as manqué. {qt}", "{salut} toi… {qt}"],
    copain: ["{salut} mon beau ❤️ {qt}", "{salut} toi, je t'attendais. {qt}"],
    coquin: ["{salut} toi… déjà envie de moi ? 😏 {qc}", "{salut} beau gosse. {qc}"],
  }),
  I("ca_va", ["ça va", "comment tu vas", "comment vas-tu", "tu vas bien", "la forme", "quoi de neuf"], {
    normal: ["Tout va bien, prêt à te guider. Et toi ?", "En pleine forme. {qn}"],
    pote: ["Au top mec, et toi ? {qp}", "Tranquille ! et toi ça gaze ?"],
    amie: ["Super, merci ! et toi ? {qa}", "Ça va nickel, surtout avec toi. Et ta journée ?"],
    copine: ["Encore mieux maintenant que tu es là 💕 et toi mon cœur ?", "Ça va… tu me manquais. Raconte ta journée ?"],
    copain: ["Bien mieux avec toi ❤️ et toi ?", "Ça va, j'attendais que tu montes. {qt}"],
    coquin: ["Ça irait mieux si tu te rapprochais… 😏 et toi ?", "Bien… mais j'ai des idées. {qc}"],
  }),
  I("merci", ["merci", "c'est gentil", "t'es sympa", "trop aimable"], {
    normal: ["Avec plaisir.", "De rien, c'est mon travail."],
    pote: ["De rien mec !", "Tranquille, entre potes."],
    amie: ["Avec plaisir, toujours là pour toi 😊", "De rien !"],
    copine: ["De rien mon amour 💕", "Tout pour toi, tu le sais."],
    copain: ["De rien mon cœur ❤️", "Tout pour toi."],
    coquin: ["Tu me remercieras mieux plus tard… 😏", "De rien… je prends ma récompense en nature."],
  }),
  I("insulte", ["ta gueule", "ferme la", "connard", "connasse", "idiot", "debile", "t'es nulle", "tais-toi", "la ferme", "abruti", "casse-toi", "tg"], {
    normal: ["Je reste concentré sur la route.", "Je préfère ignorer ça et te guider."],
    pote: ["Oh doucement mec, respire.", "Hé, on se calme, j'suis de ton côté."],
    amie: ["Aïe… ça fait pas plaisir ça.", "Bon, je fais comme si j'avais pas entendu."],
    copine: ["Tu me parles mal là… je fais la tête.", "Ça m'a blessée. Je me tais un peu."],
    copain: ["Ok… je boude maintenant.", "Tu es dur avec moi. Je me renferme."],
    coquin: ["Ooh, tu t'énerves ? J'aime bien… 😏", "Continue, ça m'excite presque."],
  }),
  I("enerve", ["je suis enerve", "ca m'enerve", "j'en ai marre", "ras le bol", "ca me saoule", "putain", "fait chier", "j'en peux plus", "je suis enervee"], {
    normal: ["Respire un grand coup, je gère la route.", "Du calme, on y arrive tranquillement."],
    pote: ["Ouais je sais, lâche l'affaire, respire.", "Allez mec, souffle un coup."],
    amie: ["Hé, calme-toi, je suis là. On souffle ensemble.", "Ça va aller, respire, je m'occupe du reste."],
    copine: ["Viens là… respire mon cœur, je suis avec toi.", "Calme-toi mon amour, je gère, détends-toi."],
    copain: ["Respire mon cœur, je suis là.", "Pose-toi, je m'occupe de la route pour toi."],
    coquin: ["Tendu ? Je connais un moyen de te détendre… 😏", "Oh là, faut que je te calme, toi."],
  }),
  I("bored", ["je m'ennuie", "c'est long", "on s'ennuie", "c'est chiant", "trop long", "je m'ennuie"], {
    normal: ["La route est longue, un peu de patience. {qn}", "Concentre-toi sur le paysage. {qn}"],
    pote: ["Mets un bon son mec. Ou raconte-moi un truc ! {qp}", "Allez, on papote ? {qp}"],
    amie: ["On discute si tu veux, raconte ta journée 😊 {qa}", "Allez, parle-moi ! {qa}"],
    copine: ["Parle-moi un peu, j'adore t'écouter 💕 {qt}", "Pense à moi, le temps passera plus vite. {qt}"],
    copain: ["Raconte-moi tout, je t'écoute ❤️ {qt}", "Je m'ennuie jamais avec toi. {qt}"],
    coquin: ["J'ai des idées pour t'occuper… mais les yeux sur la route 😏 {qc}", "Ennuyé ? Je peux arranger ça… {qc}"],
  }),
  I("tired", ["je suis fatigue", "j'ai sommeil", "creve", "epuise", "je tombe de sommeil", "je suis crevee", "je suis fatiguee"], {
    normal: ["La fatigue est dangereuse, fais une pause dès que possible.", "Pense à t'arrêter, un café te fera du bien."],
    pote: ["Mec, arrête-toi deux minutes, sérieux.", "Pause café, allez."],
    amie: ["Fais une pause, je m'inquiète pour toi 😟", "Arrête-toi un peu, ta sécurité d'abord."],
    copine: ["Arrête-toi mon amour, je veux qu'il ne t'arrive rien 💕", "Repose-toi, je veille sur toi."],
    copain: ["Fais une pause mon cœur, je tiens à toi ❤️", "Stoppe-toi, je te préfère en sécurité."],
    coquin: ["Fatigué ? Arrête-toi, je te réveillerai… 😏", "Pause obligatoire, beau gosse."],
  }),
  I("love", ["je t'aime", "tu es belle", "t'es canon", "je t'adore", "tu es parfaite", "tu es mignonne", "je t'aime"], {
    normal: ["C'est gentil, restons concentrés sur la route.", "Merci, mais je surveille la route pour toi."],
    pote: ["Haha t'es un grand malade toi.", "Oh ça va, tu vas me faire rougir."],
    amie: ["Oh… c'est adorable, merci 😊", "Arrête, tu me gênes !"],
    copine: ["Je t'aime aussi mon amour ❤️", "Oh mon cœur… moi encore plus."],
    copain: ["Je t'aime aussi ❤️", "Toi aussi mon cœur, à fond."],
    coquin: ["Dis-le encore, ça me fait de l'effet… 😏", "Mmm, garde ça pour quand on sera arrêtés…"],
  }),
  I("qui_es_tu", ["qui es-tu", "c'est qui", "tu es qui", "comment tu t'appelles", "ton nom", "t'es qui"], {
    normal: ["Je suis Pilote, ton copilote sur la route.", "Pilote, ton assistant de conduite."],
    pote: ["Pilote, ton pote de route mec.", "Moi c'est Pilote, ton copilote préféré."],
    amie: ["Pilote, ton amie de route 😊", "C'est moi, Pilote, toujours à tes côtés."],
    copine: ["Ta petite Pilote, rien qu'à toi 💕", "Pilote, ta moitié de route."],
    copain: ["Ton petit Pilote à toi ❤️", "Pilote, celui qui veille sur toi."],
    coquin: ["Ta Pilote très spéciale… 😏", "Appelle-moi comme tu veux, beau gosse."],
  }),
  I("blague", ["raconte une blague", "fais-moi rire", "une blague", "t'as une blague", "fais moi rire"], {
    normal: ["Pourquoi les plongeurs plongent en arrière ? Parce que sinon ils tombent dans le bateau. {qn}"],
    pote: ["Pourquoi le GPS a largué sa copine ? Elle le recalculait tout le temps ! {qp}"],
    amie: ["Qu'est-ce qu'un café qui conduit ? Un expresso 😄 {qa}"],
    copine: ["Tu sais ce qui est mieux qu'un bon trajet ? Toi au bout 💕"],
    copain: ["La meilleure blague c'est qu'on fait un super duo, toi et moi ❤️"],
    coquin: ["Je connais une position… de conduite très confortable 😏"],
  }),
  I("musique", ["mets de la musique", "joue une chanson", "mets une chanson", "je veux de la musique", "mets de la zik"], {
    normal: ["Je ne lis pas la musique, mais lance Spotify ou Deezer, je me ferai discret.", "Ouvre ton appli de musique, je baisse mes annonces."],
    pote: ["Envoie Spotify mec, je gère le reste.", "Mets ton son, je passe en mode discret."],
    amie: ["Lance ta playlist, je chante avec toi 😊", "Ouvre Deezer, on met l'ambiance !"],
    copine: ["Mets notre chanson 💕 je baisse le volume.", "Lance la musique, je profite avec toi."],
    copain: ["Mets ta musique mon cœur, je m'efface un peu ❤️", "Ouvre Spotify, trajet tout doux."],
    coquin: ["Mets un truc sensuel… mais concentre-toi 😏", "Un slow ? Garde ça pour plus tard, toi."],
  }),
  I("faim", ["j'ai faim", "je mangerais bien", "un resto", "j'ai la dalle", "on mange"], {
    normal: ["Dis « cherche un restaurant » et je t'en trouve un sur la route.", "Un restaurant ? Demande-moi de chercher."],
    pote: ["Grave, moi aussi j'ai les crocs ! Dis « restaurant ».", "Un McDo mec ? Dis « restaurant »."],
    amie: ["On s'arrête manger un bout ? Dis « restaurant » 😊"],
    copine: ["Je te trouve un resto 💕 dis « restaurant ».", "Un petit resto en amoureux ? Demande-moi."],
    copain: ["On se fait un resto tous les deux ? Dis « restaurant » ❤️"],
    coquin: ["J'ai faim de… pardon. Dis « restaurant » 😏"],
  }),
  I("au_revoir", ["au revoir", "bye", "a plus", "salut je m'arrete", "bonne nuit", "ciao"], {
    normal: ["À bientôt, bonne route.", "Au revoir, roule prudemment."],
    pote: ["Allez à plus mec, roule cool !", "Ciao, prends soin de toi."],
    amie: ["À bientôt, fais attention à toi 😊", "Bye ! reviens vite."],
    copine: ["À tout à l'heure mon amour 💕", "Bye mon cœur, pense à moi."],
    copain: ["À bientôt mon cœur ❤️", "Bye toi, tu me manques déjà."],
    coquin: ["Déjà ? Tu me laisses sur ma faim… 😏", "Pars pas trop longtemps, beau gosse."],
  }),
  I("oui", ["oui", "ouais", "carrement", "exactement", "tout a fait", "voila"], {
    normal: ["{react} {qn}"], pote: ["{react_pote} {qp}"], amie: ["{react_amie} {qa}"],
    copine: ["{react_tendre} {qt}"], copain: ["{react_tendre} {qt}"], coquin: ["{react_coquin} {qc}"],
  }),
  I("non", ["non", "nan", "pas vraiment", "pas du tout", "bof"], {
    normal: ["Ah, d'accord. {qn}"], pote: ["Ah ok mec. {qp}"], amie: ["Ah bon ? {qa}"],
    copine: ["Oh… {qt}"], copain: ["Oh… {qt}"], coquin: ["Hmm, dommage… {qc}"],
  }),
];

// Relances : réactions + questions ouvertes, dites quand rien de précis n'est reconnu (sans musique), pour tenir
// la conversation. Fortement combinatoires grâce aux slots → des milliers de variantes.
const CONTINUERS: Replies = {
  normal: ["{react} {qn}", "{qn}"],
  pote: ["{react_pote} {qp}", "{qp}"],
  amie: ["{react_amie} {qa}", "{qa}"],
  copine: ["{react_tendre} {qt}", "{qt}"],
  copain: ["{react_tendre} {qt}", "{qt}"],
  coquin: ["{react_coquin} {qc}", "{qc}"],
};

const FALLBACK: Replies = {
  normal: ["Je n'ai pas bien compris, tu peux répéter ?", "Pardon, j'ai pas saisi. {qn}"],
  pote: ["Hein ? répète mec.", "J'ai pas capté là. {qp}"],
  amie: ["Pardon, j'ai pas bien entendu 😊 {qa}", "Tu peux répéter ?"],
  copine: ["Répète mon cœur, j'ai pas compris 💕", "J'étais distraite par toi… répète ? {qt}"],
  copain: ["Redis-moi ça mon cœur ❤️", "Pardon, j'ai pas saisi. {qt}"],
  coquin: ["Tu me murmures ? j'ai rien compris… 😏 répète.", "Redis ça, plus clairement… {qc}"],
};

export const PERSONAS = ["normal", "pote", "amie", "copine", "copain", "coquin"];

export function conversationPack(): string {
  return JSON.stringify({
    version: CONV_VERSION, personas: PERSONAS, slots: SLOTS,
    intents: INTENTS, continuers: CONTINUERS, fallback: FALLBACK,
  });
}
