# Top da Galera — guia do projeto

App Android nativo, para uso privado entre amigos, inspirado em party games de "Top 10".
Nome provisório: **Top da Galera** (não usar o nome, logo ou cartas de jogos comerciais).

## Stack
- Kotlin + Jetpack Compose (Material 3)
- Arquitetura MVVM: `ui/` (telas Compose) → `viewmodel/` → `domain/` (regras do jogo, Kotlin puro) → `data/` (listas)
- Listas de Top 10 em JSON dentro de `app/src/main/assets/listas/`
- Sem internet nem servidor na versão 1: tudo roda offline em um único aparelho
- Testes: JUnit para `domain/` e `data/`, Compose UI Test para as telas

## Regras do jogo (versão 1 — "passa o celular")
1. 2 a 10 jogadores. Cada um começa com **3 vidas** e **0 pontos**.
2. Cada rodada sorteia um tema (uma lista de Top 10 ainda não usada na partida).
3. Em sentido horário, o jogador da vez dá um palpite em voz alta ou **passa**.
4. Qualquer outro jogador pode **contestar** o último palpite.
   - Palpite **fora** da lista: quem palpitou perde 1 vida; quem contestou ganha 1 ponto.
   - Palpite **dentro** da lista: quem contestou perde 1 vida. A posição é revelada.
5. Uma resposta já dada e confirmada não pode ser repetida.
6. A rodada termina após uma contestação, ou quando todos passam seguidos.
7. Vence quem chegar a **4 pontos** primeiro, ou o último jogador com vidas.

Vidas, pontos e quantidade de jogadores devem ficar em constantes configuráveis.

## Formato das listas (`assets/listas/*.json`)
```json
{
  "id": "paises-mais-populosos",
  "tema": "Países mais populosos do mundo",
  "categoria": "Geografia",
  "criterio": "População total estimada",
  "fontes": ["https://..."],
  "atualizado_em": "2026-10-24",
  "itens": [
    { "posicao": 1, "resposta": "Índia", "sinonimos": ["India"] },
    { "posicao": 2, "resposta": "China", "sinonimos": [] }
  ]
}
```
- Exatamente 10 itens, posições 1 a 10 sem repetição.
- `sinonimos` cobre grafias alternativas, sem acento e apelidos comuns.
- Listas pessoais do grupo (piadas internas) usam `"fontes": ["grupo"]`.

## Fluxo de trabalho com os agentes
- `pesquisador-listas`: cria e valida as listas JSON.
- `desenvolvedor-android`: implementa funcionalidades no app.
- `testador`: escreve e roda testes, compila o projeto e reporta problemas.

Ordem sugerida para cada funcionalidade: desenvolvedor implementa → testador valida →
desenvolvedor corrige o que o testador apontar.

## Comandos (Windows: use `gradlew.bat`)
- Compilar: `./gradlew assembleDebug`
- Testes unitários: `./gradlew testDebugUnitTest`
- Testes de UI (emulador/aparelho conectado): `./gradlew connectedDebugAndroidTest`
- Lint: `./gradlew lintDebug`
