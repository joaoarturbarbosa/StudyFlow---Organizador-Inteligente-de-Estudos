# StudyFlow — Organizador e Planejador de Estudos

Programa de terminal em **Java 17 puro** (sem bibliotecas externas e sem IA) que monta uma
agenda de estudos de 7 dias usando uma fórmula matemática simples de prioridade.

## Estrutura

```
StudyFlow/
├── README.md
├── src/
│   ├── Main.java               # menu de terminal (entrada e saída de dados)
│   ├── PlanejadorEstudos.java  # prioridade, distribuição das horas, validações
│   ├── Disciplina.java         # nome, dificuldade (1-5) e avaliações
│   ├── Avaliacao.java          # descrição + data (LocalDate)
│   └── SessaoEstudo.java       # disciplina + dia + minutos + concluída?
└── test/
    └── TestesStudyFlow.java    # 15 cenários, 124 verificações
```

## Como executar

Requer JDK 17 ou superior (`javac -version`).

```bash
# dentro da pasta StudyFlow
mkdir -p out
javac --release 17 -encoding UTF-8 -d out src/*.java

java -cp out Main                # usa a data de hoje
java -cp out Main 2026-11-16     # opcional: simula outra data de "hoje" (aaaa-mm-dd)
```

Para rodar os testes:

```bash
mkdir -p test-out
javac --release 17 -encoding UTF-8 -cp out -d test-out test/TestesStudyFlow.java
java -cp out:test-out TestesStudyFlow      # no Windows use ; no lugar de :
```

Dica: se os acentos aparecerem estranhos, use `java -Dstdout.encoding=UTF-8 ...`
(no Windows, rode antes `chcp 65001`).

Para um teste rápido, abra o programa, escolha **11** (exemplo de demonstração),
depois **5** (prioridades) e **6** (gerar planejamento).

## Como usar o `LocalDate`

`LocalDate` (pacote `java.time`) guarda **só a data**: ano, mês e dia, sem hora.

```java
LocalDate hoje = LocalDate.now();                 // data de hoje
LocalDate prova = LocalDate.of(2026, 11, 19);     // 19/11/2026
prova.isAfter(hoje);                              // true se a prova é depois de hoje
prova.isBefore(hoje);                             // true se a prova já passou
hoje.plusDays(3);                                 // 3 dias depois (cria um novo objeto)
ChronoUnit.DAYS.between(hoje, prova);             // quantidade de dias entre as datas
```

Dois detalhes importantes:
- `LocalDate` é **imutável**: `plusDays` devolve outra data, não altera a original.
- Para comparar datas use `equals`, `isBefore` e `isAfter`, nunca `==`.

O programa lê datas como `dd/MM/aaaa` com `ResolverStyle.STRICT`, que recusa datas
impossíveis como `31/02/2026`.

## Fórmula de prioridade

Para cada disciplina, em cada dia do plano:

```
pontosDificuldade = 2 × dificuldade                       → 2 a 10
pontosProximidade = 11 − diasAteAvaliacao (limitado 1..10) → 1 a 10
pontuação         = 4 × pontosDificuldade + 6 × pontosProximidade   → 10 a 100
```

| Dias até a prova | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 ou mais |
|---|---|---|---|---|---|---|---|---|---|---|
| pontosProximidade | 10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 |

A proximidade pesa mais (6) que a dificuldade (4): uma prova fácil que está
amanhã tende a passar à frente de uma prova difícil daqui a 10 dias.
Tudo é feito com números inteiros, então não há erros de arredondamento.

### Como as horas do dia são divididas

1. A disponibilidade diária é dividida em **blocos de 30 minutos**
   (4 h = 8 blocos).
2. Cada disciplina elegível recebe `blocos × pontuação ÷ somaDasPontuações`
   (parte inteira).
3. Os blocos que sobram vão, um a um, para quem tem o **maior resto** da divisão.
   Empate: vence a de maior pontuação; persistindo, a cadastrada primeiro.
4. Cada disciplina vira **uma sessão por dia** (blocos somados).

## Regras de validação e decisões de projeto

| Regra | Comportamento |
|---|---|
| Dificuldade | Apenas 1 a 5; fora disso lança `IllegalArgumentException` |
| Horas por dia | Não pode ser negativa, máximo 16 h, múltiplos de 0,5 h. Zero é aceito (nenhuma sessão é criada) |
| Data da avaliação | Não pode ser vazia, nem no passado, nem a mais de 730 dias |
| Mesma disciplina, mesma data | Recusado (avaliação duplicada) |
| Soma das horas do dia | Nunca passa da disponibilidade (garantido na distribuição e conferido por `validarPlanejamento()`) |
| **Avaliação no mesmo dia** | O dia da prova **não** recebe sessões daquela prova. O último dia de estudo é a véspera. Prova marcada para hoje é aceita, mas essa disciplina não recebe sessões, e o programa avisa |
| Depois da avaliação | Nenhuma sessão. Com várias avaliações da mesma disciplina, vale a próxima prova ainda por vir |
| **Duas provas no mesmo dia (disciplinas diferentes)** | Têm a mesma proximidade; a diferença vem só da dificuldade. Empate total é resolvido pela ordem de cadastro |
| Disciplina sem nenhuma avaliação | Entra com proximidade mínima (1 ponto), sem data limite |
| Gerar de novo | Apaga só as sessões **pendentes** e recria. Sessões concluídas são mantidas, o tempo delas é descontado do dia e a mesma disciplina não ganha outra sessão naquele dia |

## Exemplo: três disciplinas diferentes

Data de referência: **segunda-feira, 16/11/2026**. Disponibilidade: **4 h por dia** (8 blocos).

| Disciplina | Dificuldade | Prova | Dias até a prova |
|---|---|---|---|
| Cálculo | 5 | 19/11 | 3 |
| História | 2 | 22/11 | 6 |
| Programação | 3 | 28/11 | 12 |

### Dia 1 — cálculo passo a passo

| Disciplina | pontosDif | pontosProx | Pontuação |
|---|---|---|---|
| Cálculo | 2×5 = 10 | 11−3 = 8 | 4×10 + 6×8 = **88** |
| História | 2×2 = 4 | 11−6 = 5 | 4×4 + 6×5 = **46** |
| Programação | 2×3 = 6 | 11−12 → mínimo **1** | 4×6 + 6×1 = **30** |

Soma = 164. Divisão dos 8 blocos:

| Disciplina | 8 × pontos ÷ 164 | Parte inteira | Resto |
|---|---|---|---|
| Cálculo | 704 ÷ 164 = 4,29 | 4 | 48 |
| História | 368 ÷ 164 = 2,24 | 2 | 40 |
| Programação | 240 ÷ 164 = 1,46 | 1 | 76 |

Foram distribuídos 4 + 2 + 1 = 7 blocos. Sobrou 1, que vai para o maior resto
(Programação, 76). Resultado: **Cálculo 2h, História 1h, Programação 1h.**

### Dia 4 (19/11) — dia da prova de Cálculo

Cálculo não é mais elegível (prova no próprio dia). Restam duas disciplinas:
História `4×4 + 6×8 = 64` (prova em 3 dias) e Programação `4×6 + 6×2 = 36` (prova em 9 dias).
Blocos: 8 × 64 ÷ 100 = 5,12 → 5 (resto 12) e 8 × 36 ÷ 100 = 2,88 → 2 (resto 88).
Sobra 1 bloco para Programação: **História 2h30, Programação 1h30.**

### Plano completo gerado

| Dia | Cálculo | História | Programação | Total |
|---|---|---|---|---|
| 1 — seg 16/11 | 2h00 | 1h00 | 1h00 | 4h |
| 2 — ter 17/11 | 2h00 | 1h30 | 0h30 | 4h |
| 3 — qua 18/11 | 2h00 | 1h30 | 0h30 | 4h |
| 4 — qui 19/11 (prova de Cálculo) | — | 2h30 | 1h30 | 4h |
| 5 — sex 20/11 | — | 2h30 | 1h30 | 4h |
| 6 — sáb 21/11 | — | 2h30 | 1h30 | 4h |
| 7 — dom 22/11 (prova de História) | — | — | 4h00 | 4h |

Horas atribuídas: Cálculo **6h00**, História **11h30**, Programação **10h30** (total 28h00).

Repare como a prioridade se move: o Cálculo domina enquanto a prova está perto,
e as horas dele são redistribuídas quando a prova passa.

## Como modificar as regras de prioridade

Todas as regras estão no começo de `PlanejadorEstudos.java`, nas constantes `public static final`:

| Quero... | Altere |
|---|---|
| Dar mais peso à dificuldade | `PESO_DIFICULDADE` (ex.: 6) e `PESO_PROXIMIDADE` (ex.: 4) |
| Blocos de 15 ou 60 minutos | `BLOCO_MINUTOS` (a disponibilidade deve ser múltipla dele) |
| Mudar o limite diário | `MAX_MINUTOS_POR_DIA` |
| Planejar mais dias | `DIAS_PLANEJAMENTO` |
| Mudar a janela de urgência (hoje, 10 dias) | o método `calcularPontosProximidade`, na linha `11 - dias`. Com `16 - dias` e limite máximo 15, a urgência começa 15 dias antes |
| Urgência mais agressiva perto da prova | em `calcularPontosProximidade`, troque a conta linear, por exemplo, `pontos = (dias <= 2) ? 10 : 11 - dias` |

Depois de alterar, recompile e rode os testes. Os valores esperados dos cenários 01, 02 e 14
usam os pesos 4 e 6, então esses testes precisarão ser atualizados com os novos cálculos.

## Cenários de teste automáticos

| # | Cenário |
|---|---|
| 01 | Pontuação com exemplos concretos (88, 46, 30) e limites da proximidade |
| 02 | Distribuição do dia 1 (2h, 1h, 1h) e regra do maior resto |
| 03 | Soma diária igual à disponibilidade para 0,5 / 1 / 2,5 / 4 / 8 / 16 h |
| 04 | Nenhuma sessão no dia da prova ou depois |
| 05 | Avaliação no dia de hoje: aceita, sem sessões para ela |
| 06 | Duas provas no mesmo dia: empate e desempate |
| 07 | Gerar três vezes não duplica sessões |
| 08 | Dificuldade 0, 6 e negativa; nome vazio ou repetido; horas negativas, NaN, 17 h e 0,25 h |
| 09 | Datas: passado, nula, distante, repetida e 31/02 |
| 10 | Regerar mantém sessões concluídas e respeita a nova disponibilidade |
| 11 | Progresso (14%) e soma das horas por disciplina |
| 12 | Disponibilidade zero |
| 13 | Prova mais próxima e disciplina mais difícil recebem mais tempo |
| 14 | Disciplina sem avaliação (pontuação 22) |
| 15 | Marcar sessão inexistente, já concluída, dia 0 e dia 8 |

## Testes manuais recomendados (menu)

O menu foi exercitado aqui com entradas inválidas. Confira você mesmo:

- Texto no lugar de número (`abc`) em qualquer opção → mensagem de erro, sem travar.
- Dificuldade `0` ou `6`; horas `-2`; data `31/02/2026`; data no passado.
- Opção 6 sem disciplinas cadastradas → aviso pedindo o cadastro.
- Opção 4 com um plano existente → o plano é regenerado automaticamente.
- Opção 8: concluir uma sessão, tentar concluir de novo (erro) e ver o progresso (opção 9).
- Alterar dificuldade (opção 3) → o programa avisa que é preciso gerar de novo (opção 6).

## Limitações conhecidas

- Os dados ficam só na memória. Ao fechar o programa, tudo se perde (extensão natural: salvar em arquivo).
- A distribuição é feita dia a dia pela pontuação do dia. Ela não garante um mínimo de horas
  por disciplina antes da prova. Se isso for importante, o próximo passo é calcular uma meta
  total de horas por disciplina e distribuir as sessões para cumpri-la.
- A disponibilidade é a mesma para todos os dias da semana.
