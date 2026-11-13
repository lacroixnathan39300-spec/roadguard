# Format du pack de conversation de Pilote

Ce document explique comment écrire un **pack de conversation** pour Pilote (le GPS). Tu peux le donner tel quel à
une autre IA pour qu'elle génère un gros pack ; il suffit qu'elle respecte le format JSON ci-dessous. Nathan me
transmet ensuite le fichier et je l'intègre.

## À quoi sert le pack

Pilote écoute au micro (hors ligne), transforme ta voix en texte, puis **cherche des mots-clés** dans ce texte pour
choisir une réponse. Le pack contient toutes les réponses possibles, par **personnage** (persona). La voix du
téléphone lit ensuite la réponse. Rien n'est envoyé sur Internet.

## Le fichier : un seul objet JSON

```json
{
  "version": 5,
  "personas": ["normal", "pote", "amie", "copine", "copain", "coquin"],
  "slots": { "...": ["...", "..."] },
  "intents": [ { "id": "...", "kw": ["..."], "r": { "persona": ["..."] } } ],
  "continuers": { "persona": ["..."] },
  "fallback": { "persona": ["..."] }
}
```

- **version** (nombre) : augmente-le à chaque nouveau pack, sinon les téléphones ne le re-téléchargent pas.
- **personas** : la liste des personnages. Garde exactement ceux-ci :
  - `normal` : GPS neutre et poli
  - `pote` : un pote (garçon), décontracté
  - `amie` : une amie (fille), chaleureuse
  - `copine` : une petite amie (tendre)
  - `copain` : un petit copain (tendre, pour les conductrices)
  - `coquin` : une sex-friend très taquine (ton « Coquin XXL », jamais explicite/sexuel cru)
- **slots**, **intents**, **continuers**, **fallback** : détaillés ci-dessous.

## Les intentions (`intents`)

Chaque intention = une situation reconnue par mots-clés, avec des réponses par personnage.

```json
{
  "id": "salut",
  "kw": ["bonjour", "salut", "coucou", "hello", "bonsoir"],
  "r": {
    "normal": ["Bonjour ! En route quand tu veux."],
    "pote":   ["Yo ! ça roule ?", "Salut mec !"],
    "amie":   ["Coucou toi ! contente de rouler avec toi."],
    "copine": ["Coucou mon cœur ❤️ tu m'as manqué."],
    "copain": ["Salut mon beau ❤️"],
    "coquin": ["Salut toi… déjà envie de moi ? 😏"]
  }
}
```

Règles :
- **id** : identifiant court et unique (lettres, chiffres, `_`).
- **kw** : les mots ou expressions qui déclenchent l'intention. En **minuscules, sans accents** (é→e, ç→c…),
  sans ponctuation. Ils sont cherchés comme des **mots entiers** dans ce que dit le conducteur. Mets beaucoup de
  variantes (« ça va », « comment tu vas », « tu vas bien »…).
- **r** : les réponses, par personnage. **`normal` sert de secours** : si un personnage n'a pas de réponse propre
  pour une intention, Pilote prend celle de `normal`. Tu peux donc ne remplir que `normal` + les personnages où le
  ton change vraiment.
- Mets **plusieurs réponses** par personnage (Pilote en choisit une au hasard et évite de répéter).

## Les trous `{slot}` (pour multiplier la variété)

Dans n'importe quelle réponse, tu peux écrire `{nom_de_slot}`. Pilote le remplace au hasard par un élément de la
liste `slots["nom_de_slot"]`. Ça permet, avec des listes courtes, d'obtenir **des milliers de phrases différentes**.

```json
"slots": {
  "react_pote": ["Ah ouais ?", "Sérieux ?", "Haha.", "Grave."],
  "qp": ["Et après ?", "Raconte mec !", "Et sinon toi ?", "T'en penses quoi ?"]
},
"intents": [
  { "id": "oui", "kw": ["oui", "ouais", "carrement"], "r": { "pote": ["{react_pote} {qp}"] } }
]
```

Ici « {react_pote} {qp} » donne 4 × 4 = 16 phrases ; avec plus d'éléments, ça monte très vite. Un slot peut
contenir ce que tu veux (réactions, questions, surnoms, interjections…). Les slots sont communs à tous les
personnages, donc garde-les plutôt neutres et mets le ton propre au personnage dans la réponse elle-même.

## Les relances (`continuers`) — pour tenir une conversation de plusieurs minutes

Quand le conducteur dit quelque chose que **aucune** intention ne reconnaît (et qu'il n'y a pas de musique), Pilote
pioche une **relance** : une réaction + une question ouverte, pour garder la conversation. C'est ce qui permet de
discuter longtemps. Mets-en beaucoup (idéalement avec des slots).

```json
"continuers": {
  "normal": ["{react} {qn}", "{qn}"],
  "pote":   ["{react_pote} {qp}", "{qp}"],
  "amie":   ["{react_amie} {qa}"],
  "copine": ["{react_tendre} {qt}"],
  "copain": ["{react_tendre} {qt}"],
  "coquin": ["{react_coquin} {qc}"]
}
```

## Le secours (`fallback`)

Dit quand Pilote n'a vraiment rien compris (ou quand il y a de la musique et que ce n'est pas une vraie question).
Même structure : par personnage.

```json
"fallback": {
  "normal": ["Je n'ai pas bien compris, tu peux répéter ?"],
  "pote":   ["Hein ? répète mec."]
}
```

## Les questions « en direct » : NE PAS les mettre dans le pack

Certaines questions sont gérées par le code de Pilote (il a les données en temps réel), pas par le pack :
vitesse actuelle, limitation, heure, prochain radar, temps/distance restants, « on est où ». Inutile d'écrire des
réponses pour ça — Pilote répond tout seul avec les vraies valeurs.

## Consignes de ton et de contenu

- Français naturel, phrases **courtes** (c'est lu à voix haute en voiture).
- **Sécurité d'abord** : jamais d'encouragement à rouler vite, à forcer malgré la fatigue, etc.
- Personnage `coquin` : taquin, suggestif, **jamais explicite ni sexuel cru**, et toujours « garde les yeux sur la
  route ». Pas de contenu sexuel détaillé.
- Pas d'insultes gratuites envers le conducteur, pas de contenu haineux.
- Les emojis sont permis (ils ne sont pas lus à voix haute).

## Taille

Le texte reste léger : même un très gros pack fait quelques Mo, pas des Go. Vise la **variété** (beaucoup de slots et
de continuers) plutôt que la taille du fichier : c'est la combinaison slots + anti-répétition qui évite d'entendre
deux fois la même phrase.

## Ce qu'il faut rendre

Un seul fichier **`conversation.json`** valide (un seul objet JSON, encodage UTF-8), respectant exactement les clés
ci-dessus. Nathan me le donne, je le mets sur le serveur, et toutes les applis le téléchargent.
