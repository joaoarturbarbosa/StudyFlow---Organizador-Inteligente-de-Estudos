import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

/**
 * Cérebro do StudyFlow: guarda disciplinas e sessões, calcula prioridades
 * e distribui as horas de estudo nos próximos 7 dias.
 *
 * FÓRMULA DE PRIORIDADE (para uma disciplina em um dia):
 *
 *   pontosDificuldade  = 2 x dificuldade                      (2 a 10)
 *   pontosProximidade  = 11 - diasAteAvaliacao, limitado a 1..10
 *   pontuacao          = PESO_DIFICULDADE x pontosDificuldade
 *                      + PESO_PROXIMIDADE x pontosProximidade (10 a 100)
 *
 * Só números inteiros são usados, então não há erro de arredondamento.
 */
public class PlanejadorEstudos {

    // ---- Regras que podem ser alteradas (veja o README) ----
    public static final int DIAS_PLANEJAMENTO = 7;
    public static final int BLOCO_MINUTOS = 30;            // menor fatia de estudo
    public static final int MAX_MINUTOS_POR_DIA = 16 * 60; // limite de horas por dia
    public static final int PESO_DIFICULDADE = 4;
    public static final int PESO_PROXIMIDADE = 6;
    public static final int MAX_DIAS_ATE_AVALIACAO = 730;  // ~2 anos

    // STRICT recusa datas impossíveis, como 31/02/2026
    public static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final LocalDate hoje; // data de referência: o plano começa nela
    private final ArrayList<Disciplina> disciplinas = new ArrayList<>();
    private final ArrayList<SessaoEstudo> sessoes = new ArrayList<>();
    private int disponibilidadeMinutos = 0;
    private int proximoId = 1;
    private boolean desatualizado = false;

    public PlanejadorEstudos() {
        this(LocalDate.now());
    }

    /** Permite informar a data de "hoje" (útil para testes e demonstrações). */
    public PlanejadorEstudos(LocalDate hoje) {
        if (hoje == null) {
            throw new IllegalArgumentException("A data de referência não pode ser vazia.");
        }
        this.hoje = hoje;
    }

    // ------------------------------------------------------------------
    // Cadastros e validações
    // ------------------------------------------------------------------

    public Disciplina cadastrarDisciplina(String nome, int dificuldade) {
        Disciplina nova = new Disciplina(nome, dificuldade); // valida nome e dificuldade
        if (buscarDisciplina(nova.getNome()) != null) {
            throw new IllegalArgumentException("Já existe a disciplina " + nova.getNome() + ".");
        }
        disciplinas.add(nova);
        desatualizado = true;
        return nova;
    }

    public Disciplina buscarDisciplina(String nome) {
        if (nome == null) {
            return null;
        }
        for (Disciplina d : disciplinas) {
            if (d.getNome().equalsIgnoreCase(nome.trim())) {
                return d;
            }
        }
        return null;
    }

    public void alterarDificuldade(Disciplina disciplina, int novaDificuldade) {
        disciplina.setDificuldade(novaDificuldade);
        desatualizado = true;
    }

    /**
     * Regras da data: não pode ser vazia, não pode estar no passado e não pode
     * estar a mais de MAX_DIAS_ATE_AVALIACAO dias. Avaliação HOJE é aceita,
     * mas não gera sessões (veja gerarPlanejamento).
     */
    public Avaliacao registrarAvaliacao(Disciplina disciplina, LocalDate data, String descricao) {
        if (disciplina == null) {
            throw new IllegalArgumentException("Escolha uma disciplina válida.");
        }
        if (data == null) {
            throw new IllegalArgumentException("A data da avaliação não pode ser vazia.");
        }
        if (data.isBefore(hoje)) {
            throw new IllegalArgumentException("A data da avaliação não pode estar no passado.");
        }
        if (ChronoUnit.DAYS.between(hoje, data) > MAX_DIAS_ATE_AVALIACAO) {
            throw new IllegalArgumentException("A data da avaliação está distante demais (máx. "
                    + MAX_DIAS_ATE_AVALIACAO + " dias).");
        }
        Avaliacao avaliacao = new Avaliacao(descricao, data);
        disciplina.adicionarAvaliacao(avaliacao);
        desatualizado = true;
        return avaliacao;
    }

    /** Informa as horas por dia (aceita múltiplos de 0,5 h). */
    public void definirDisponibilidadeHoras(double horas) {
        if (Double.isNaN(horas) || Double.isInfinite(horas)) {
            throw new IllegalArgumentException("Valor de horas inválido.");
        }
        if (horas < 0) {
            throw new IllegalArgumentException("As horas de estudo não podem ser negativas.");
        }
        long minutos = Math.round(horas * 60);
        if (minutos > MAX_MINUTOS_POR_DIA) {
            throw new IllegalArgumentException("O máximo é " + (MAX_MINUTOS_POR_DIA / 60)
                    + " horas de estudo por dia.");
        }
        if (minutos % BLOCO_MINUTOS != 0) {
            throw new IllegalArgumentException("Use múltiplos de 0,5 hora (30 minutos).");
        }
        this.disponibilidadeMinutos = (int) minutos;
        desatualizado = true;
    }

    // ------------------------------------------------------------------
    // Prioridade
    // ------------------------------------------------------------------

    public int calcularPontosDificuldade(Disciplina d) {
        return 2 * d.getDificuldade();
    }

    /**
     * Quanto mais perto a avaliação, mais pontos (de 1 a 10).
     * 1 dia = 10 pontos, 2 dias = 9, ... 10 dias ou mais = 1.
     * Sem avaliação futura registrada, vale o mínimo: 1 ponto.
     */
    public int calcularPontosProximidade(Disciplina d, LocalDate dia) {
        Avaliacao proxima = d.proximaAvaliacaoApos(dia);
        if (proxima == null) {
            return 1;
        }
        long dias = ChronoUnit.DAYS.between(dia, proxima.getData());
        long pontos = 11 - dias;
        return (int) Math.max(1, Math.min(10, pontos));
    }

    public int calcularPontuacao(Disciplina d, LocalDate dia) {
        return PESO_DIFICULDADE * calcularPontosDificuldade(d)
                + PESO_PROXIMIDADE * calcularPontosProximidade(d, dia);
    }

    /**
     * Uma disciplina só entra no dia se: (a) nunca teve avaliação registrada, ou
     * (b) ainda existe uma avaliação DEPOIS desse dia. Assim nunca se estuda
     * na data da prova nem depois dela.
     */
    public boolean disciplinaElegivel(Disciplina d, LocalDate dia) {
        return !d.temAvaliacoes() || d.proximaAvaliacaoApos(dia) != null;
    }

    /** Disciplinas elegíveis no dia, da maior para a menor pontuação. */
    public ArrayList<Disciplina> ordenarPorPrioridade(LocalDate dia) {
        ArrayList<Disciplina> lista = new ArrayList<>();
        for (Disciplina d : disciplinas) {
            if (disciplinaElegivel(d, dia)) {
                lista.add(d);
            }
        }
        lista.sort((a, b) -> calcularPontuacao(b, dia) - calcularPontuacao(a, dia));
        return lista;
    }

    // ------------------------------------------------------------------
    // Geração do planejamento
    // ------------------------------------------------------------------

    /**
     * Gera o plano de 7 dias a partir de "hoje".
     * - Apaga só as sessões NÃO concluídas (por isso não duplica ao gerar de novo).
     * - Sessões concluídas são mantidas; o tempo delas é descontado do dia, e a
     *   disciplina que já tem sessão concluída naquele dia não recebe outra.
     * - Em cada dia, os blocos de 30 min são divididos proporcionalmente à pontuação.
     */
    public void gerarPlanejamento() {
        if (disciplinas.isEmpty()) {
            throw new IllegalStateException("Cadastre pelo menos uma disciplina antes de gerar o plano.");
        }
        sessoes.removeIf(s -> !s.isConcluida());

        for (int i = 0; i < DIAS_PLANEJAMENTO; i++) {
            LocalDate dia = hoje.plusDays(i);
            int livres = Math.max(0, disponibilidadeMinutos - minutosNoDia(dia, true));
            int blocos = livres / BLOCO_MINUTOS;

            ArrayList<Disciplina> candidatas = new ArrayList<>();
            for (Disciplina d : disciplinas) {
                if (disciplinaElegivel(d, dia) && !temSessaoNoDia(d, dia)) {
                    candidatas.add(d);
                }
            }
            if (blocos == 0 || candidatas.isEmpty()) {
                continue;
            }

            int[] pontos = new int[candidatas.size()];
            for (int k = 0; k < pontos.length; k++) {
                pontos[k] = calcularPontuacao(candidatas.get(k), dia);
            }
            int[] blocosPorDisciplina = distribuirBlocos(blocos, pontos);

            for (int k = 0; k < candidatas.size(); k++) {
                if (blocosPorDisciplina[k] > 0) {
                    sessoes.add(new SessaoEstudo(proximoId++, candidatas.get(k), dia,
                            blocosPorDisciplina[k] * BLOCO_MINUTOS));
                }
            }
        }
        validarPlanejamento();
        desatualizado = false;
    }

    /**
     * Divide "blocos" proporcionalmente aos pontos (método do maior resto).
     * 1) cada disciplina recebe a parte inteira de blocos x pontos / somaPontos;
     * 2) os blocos que sobraram vão para quem tem o maior resto;
     * 3) empate no resto: vence a de mais pontos; persistindo, a cadastrada primeiro.
     */
    static int[] distribuirBlocos(int blocos, int[] pontos) {
        int n = pontos.length;
        int soma = 0;
        for (int p : pontos) {
            soma += p;
        }
        int[] resultado = new int[n];
        int[] resto = new int[n];
        int distribuidos = 0;
        for (int k = 0; k < n; k++) {
            resultado[k] = (blocos * pontos[k]) / soma;
            resto[k] = (blocos * pontos[k]) % soma;
            distribuidos += resultado[k];
        }
        int sobra = blocos - distribuidos; // sempre menor que n
        boolean[] jaRecebeu = new boolean[n];
        for (int rodada = 0; rodada < sobra; rodada++) {
            int melhor = -1;
            for (int k = 0; k < n; k++) {
                if (jaRecebeu[k]) {
                    continue;
                }
                if (melhor == -1 || resto[k] > resto[melhor]
                        || (resto[k] == resto[melhor] && pontos[k] > pontos[melhor])) {
                    melhor = k;
                }
            }
            resultado[melhor]++;
            jaRecebeu[melhor] = true;
        }
        return resultado;
    }

    /** Autoverificação: lança erro se alguma regra do plano estiver quebrada. */
    public void validarPlanejamento() {
        for (int i = 0; i < DIAS_PLANEJAMENTO; i++) {
            LocalDate dia = hoje.plusDays(i);
            int total = minutosNoDia(dia, false);
            int concluidos = minutosNoDia(dia, true);
            if (total > Math.max(disponibilidadeMinutos, concluidos)) {
                throw new IllegalStateException("O dia " + dia.format(FORMATO_DATA)
                        + " passou da disponibilidade.");
            }
        }
        for (int a = 0; a < sessoes.size(); a++) {
            SessaoEstudo s = sessoes.get(a);
            Disciplina d = s.getDisciplina();
            if (d.temAvaliacoes() && d.proximaAvaliacaoApos(s.getData()) == null) {
                throw new IllegalStateException("Sessão de " + d.getNome()
                        + " em data igual ou posterior à última avaliação.");
            }
            for (int b = a + 1; b < sessoes.size(); b++) {
                SessaoEstudo outra = sessoes.get(b);
                if (outra.getDisciplina() == d && outra.getData().equals(s.getData())) {
                    throw new IllegalStateException("Sessão duplicada de " + d.getNome()
                            + " em " + s.getData().format(FORMATO_DATA));
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Consultas, conclusão e progresso
    // ------------------------------------------------------------------

    public LocalDate dataDoDia(int numeroDia) {
        if (numeroDia < 1 || numeroDia > DIAS_PLANEJAMENTO) {
            throw new IllegalArgumentException("Escolha um dia de 1 a " + DIAS_PLANEJAMENTO + ".");
        }
        return hoje.plusDays(numeroDia - 1);
    }

    public ArrayList<SessaoEstudo> consultarDia(int numeroDia) {
        LocalDate dia = dataDoDia(numeroDia);
        ArrayList<SessaoEstudo> doDia = new ArrayList<>();
        for (SessaoEstudo s : sessoes) {
            if (s.getData().equals(dia)) {
                doDia.add(s);
            }
        }
        return doDia;
    }

    public void marcarConcluida(int id) {
        for (SessaoEstudo s : sessoes) {
            if (s.getId() == id) {
                s.concluir();
                return;
            }
        }
        throw new IllegalArgumentException("Não existe sessão com o número " + id + ".");
    }

    public int minutosPlanejados(Disciplina d) {
        int total = 0;
        for (SessaoEstudo s : sessoes) {
            if (s.getDisciplina() == d) {
                total += s.getMinutos();
            }
        }
        return total;
    }

    public int minutosConcluidos(Disciplina d) {
        int total = 0;
        for (SessaoEstudo s : sessoes) {
            if (s.getDisciplina() == d && s.isConcluida()) {
                total += s.getMinutos();
            }
        }
        return total;
    }

    public int minutosPlanejadosTotal() {
        int total = 0;
        for (SessaoEstudo s : sessoes) {
            total += s.getMinutos();
        }
        return total;
    }

    public int minutosConcluidosTotal() {
        int total = 0;
        for (SessaoEstudo s : sessoes) {
            if (s.isConcluida()) {
                total += s.getMinutos();
            }
        }
        return total;
    }

    /** Percentual inteiro (0 a 100). Sem sessões, devolve 0. */
    public static int percentual(int parte, int total) {
        if (total == 0) {
            return 0;
        }
        return parte * 100 / total;
    }

    private int minutosNoDia(LocalDate dia, boolean somenteConcluidas) {
        int total = 0;
        for (SessaoEstudo s : sessoes) {
            if (s.getData().equals(dia) && (!somenteConcluidas || s.isConcluida())) {
                total += s.getMinutos();
            }
        }
        return total;
    }

    private boolean temSessaoNoDia(Disciplina d, LocalDate dia) {
        for (SessaoEstudo s : sessoes) {
            if (s.getDisciplina() == d && s.getData().equals(dia)) {
                return true;
            }
        }
        return false;
    }

    // ---- getters ----

    public LocalDate getHoje() {
        return hoje;
    }

    public ArrayList<Disciplina> getDisciplinas() {
        return new ArrayList<>(disciplinas);
    }

    public ArrayList<SessaoEstudo> getSessoes() {
        return new ArrayList<>(sessoes);
    }

    public int getDisponibilidadeMinutos() {
        return disponibilidadeMinutos;
    }

    public boolean isDesatualizado() {
        return desatualizado;
    }

    public static String formatarMinutos(int minutos) {
        return (minutos / 60) + "h" + String.format("%02d", minutos % 60);
    }
}