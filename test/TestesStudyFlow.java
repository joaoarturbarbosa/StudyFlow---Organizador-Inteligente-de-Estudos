import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Testes automáticos sem biblioteca externa.
 * Executar: java -cp out:test-out TestesStudyFlow   (veja o README)
 */
public class TestesStudyFlow {
    private static int passou = 0;
    private static int falhou = 0;
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 16);

    public static void main(String[] args) {
        cenario01PontuacaoComExemplosConcretos();
        cenario02DistribuicaoDoPrimeiroDia();
        cenario03SomaNuncaExcedeDisponibilidade();
        cenario04NenhumaSessaoNoDiaDaProvaOuDepois();
        cenario05AvaliacaoNoMesmoDiaDeHoje();
        cenario06DuasAvaliacoesNoMesmoDia();
        cenario07GerarDeNovoNaoDuplica();
        cenario08ValoresInvalidosSaoRejeitados();
        cenario09DatasInvalidasSaoRejeitadas();
        cenario10RegerarPreservaConcluidas();
        cenario11ProgressoEHorasPorDisciplina();
        cenario12DisponibilidadeZero();
        cenario13ProvaMaisProximaRecebeMais();
        cenario14DisciplinaSemAvaliacao();
        cenario15ConclusaoInvalida();

        System.out.println();
        System.out.println("Resultado: " + passou + " verificações OK, " + falhou + " falhas.");
        if (falhou > 0) {
            System.exit(1);
        }
    }

    // ---------- utilidades ----------

    private static void verificar(String descricao, boolean condicao) {
        if (condicao) {
            passou++;
        } else {
            falhou++;
            System.out.println("  FALHOU: " + descricao);
        }
    }

    private static void verificarIgual(String descricao, int esperado, int obtido) {
        verificar(descricao + " (esperado " + esperado + ", obtido " + obtido + ")", esperado == obtido);
    }

    private static void esperarErro(String descricao, Runnable acao) {
        try {
            acao.run();
            verificar(descricao + " deveria lançar exceção", false);
        } catch (IllegalArgumentException | IllegalStateException e) {
            verificar(descricao, true);
        }
    }

    private static void titulo(String nome) {
        System.out.println(nome);
    }

    /** Cenário base: Cálculo (5, prova em 3 dias), História (2, 6 dias), Programação (3, 12 dias), 4h/dia. */
    private static PlanejadorEstudos cenarioBase() {
        PlanejadorEstudos p = new PlanejadorEstudos(HOJE);
        Disciplina calc = p.cadastrarDisciplina("Cálculo", 5);
        Disciplina hist = p.cadastrarDisciplina("História", 2);
        Disciplina prog = p.cadastrarDisciplina("Programação", 3);
        p.registrarAvaliacao(calc, HOJE.plusDays(3), "Prova 1");
        p.registrarAvaliacao(hist, HOJE.plusDays(6), "Prova 1");
        p.registrarAvaliacao(prog, HOJE.plusDays(12), "Prova 1");
        p.definirDisponibilidadeHoras(4);
        return p;
    }

    private static int minutosDoDia(PlanejadorEstudos p, int dia) {
        int total = 0;
        for (SessaoEstudo s : p.consultarDia(dia)) {
            total += s.getMinutos();
        }
        return total;
    }

    private static int minutosDaDisciplinaNoDia(PlanejadorEstudos p, int dia, String nome) {
        for (SessaoEstudo s : p.consultarDia(dia)) {
            if (s.getDisciplina().getNome().equals(nome)) {
                return s.getMinutos();
            }
        }
        return 0;
    }

    // ---------- cenários ----------

    static void cenario01PontuacaoComExemplosConcretos() {
        titulo("Cenário 01 - pontuação com exemplos concretos");
        PlanejadorEstudos p = cenarioBase();
        Disciplina calc = p.buscarDisciplina("Cálculo");
        Disciplina hist = p.buscarDisciplina("História");
        Disciplina prog = p.buscarDisciplina("Programação");
        // Cálculo: 4 x (2x5) + 6 x (11-3) = 40 + 48 = 88
        verificarIgual("Cálculo", 88, p.calcularPontuacao(calc, HOJE));
        // História: 4 x (2x2) + 6 x (11-6) = 16 + 30 = 46
        verificarIgual("História", 46, p.calcularPontuacao(hist, HOJE));
        // Programação: 4 x (2x3) + 6 x 1 (12 dias, limitado a 1) = 24 + 6 = 30
        verificarIgual("Programação", 30, p.calcularPontuacao(prog, HOJE));
        // limites da proximidade: 1 dia = 10, 10 dias = 1, 11 dias = 1
        verificarIgual("1 dia = 10 pontos", 10, p.calcularPontosProximidade(calc, HOJE.plusDays(2)));
        verificarIgual("12 dias = 1 ponto", 1, p.calcularPontosProximidade(prog, HOJE));
        // ordem de prioridade
        ArrayList<Disciplina> ordem = p.ordenarPorPrioridade(HOJE);
        verificar("ordem Cálculo > História > Programação",
                ordem.get(0) == calc && ordem.get(1) == hist && ordem.get(2) == prog);
    }

    static void cenario02DistribuicaoDoPrimeiroDia() {
        titulo("Cenário 02 - distribuição do dia 1 (8 blocos de 30 min)");
        PlanejadorEstudos p = cenarioBase();
        p.gerarPlanejamento();
        // quotas: 8x88/164=4,29 | 8x46/164=2,24 | 8x30/164=1,46 -> 4+2+1 = 7, sobra 1
        // restos: Cálculo 48, História 40, Programação 76 -> o bloco que sobrou vai para Programação
        verificarIgual("Cálculo dia 1", 120, minutosDaDisciplinaNoDia(p, 1, "Cálculo"));
        verificarIgual("História dia 1", 60, minutosDaDisciplinaNoDia(p, 1, "História"));
        verificarIgual("Programação dia 1", 60, minutosDaDisciplinaNoDia(p, 1, "Programação"));
        verificarIgual("método distribuirBlocos soma", 8,
                soma(PlanejadorEstudos.distribuirBlocos(8, new int[]{88, 46, 30})));
    }

    private static int soma(int[] v) {
        int t = 0;
        for (int x : v) {
            t += x;
        }
        return t;
    }

    static void cenario03SomaNuncaExcedeDisponibilidade() {
        titulo("Cenário 03 - soma diária nunca excede a disponibilidade");
        double[] opcoes = {0.5, 1, 2.5, 4, 8, 16};
        for (double horas : opcoes) {
            PlanejadorEstudos p = cenarioBase();
            p.definirDisponibilidadeHoras(horas);
            p.gerarPlanejamento();
            int limite = (int) Math.round(horas * 60);
            for (int dia = 1; dia <= 7; dia++) {
                verificarIgual(horas + "h, dia " + dia + " usa toda a disponibilidade",
                        limite, minutosDoDia(p, dia));
            }
        }
    }

    static void cenario04NenhumaSessaoNoDiaDaProvaOuDepois() {
        titulo("Cenário 04 - nenhuma sessão no dia da prova ou depois");
        PlanejadorEstudos p = cenarioBase();
        p.gerarPlanejamento();
        for (SessaoEstudo s : p.getSessoes()) {
            Disciplina d = s.getDisciplina();
            LocalDate prova = d.getAvaliacoes().get(0).getData();
            verificar(d.getNome() + " em " + s.getData() + " é antes da prova " + prova,
                    s.getData().isBefore(prova));
        }
        verificarIgual("Cálculo não aparece no dia 4 (dia da prova é 19/11)", 0,
                minutosDaDisciplinaNoDia(p, 4, "Cálculo"));
        verificar("Cálculo tem sessão no dia 3 (véspera)", minutosDaDisciplinaNoDia(p, 3, "Cálculo") > 0);
    }

    static void cenario05AvaliacaoNoMesmoDiaDeHoje() {
        titulo("Cenário 05 - avaliação hoje não gera sessões para ela");
        PlanejadorEstudos p = new PlanejadorEstudos(HOJE);
        Disciplina a = p.cadastrarDisciplina("Física", 4);
        Disciplina b = p.cadastrarDisciplina("Inglês", 2);
        p.registrarAvaliacao(a, HOJE, "Prova hoje"); // aceita
        p.registrarAvaliacao(b, HOJE.plusDays(5), "Prova 1");
        p.definirDisponibilidadeHoras(3);
        p.gerarPlanejamento();
        verificarIgual("Física sem horas", 0, p.minutosPlanejados(a));
        verificarIgual("Inglês recebe tudo no dia 1", 180, minutosDaDisciplinaNoDia(p, 1, "Inglês"));
    }

    static void cenario06DuasAvaliacoesNoMesmoDia() {
        titulo("Cenário 06 - duas avaliações no mesmo dia (empate)");
        PlanejadorEstudos p = new PlanejadorEstudos(HOJE);
        Disciplina a = p.cadastrarDisciplina("A", 3);
        Disciplina b = p.cadastrarDisciplina("B", 3);
        p.registrarAvaliacao(a, HOJE.plusDays(2), "P1");
        p.registrarAvaliacao(b, HOJE.plusDays(2), "P1");
        p.definirDisponibilidadeHoras(4);
        p.gerarPlanejamento();
        verificarIgual("A igual a B com 8 blocos", minutosDaDisciplinaNoDia(p, 1, "A"),
                minutosDaDisciplinaNoDia(p, 1, "B"));
        p.definirDisponibilidadeHoras(1.5); // 3 blocos: 1,5 para cada -> sobra vai para a cadastrada primeiro
        p.gerarPlanejamento();
        verificarIgual("desempate: A (cadastrada primeiro) fica com 2 blocos", 60,
                minutosDaDisciplinaNoDia(p, 1, "A"));
        verificarIgual("desempate: B fica com 1 bloco", 30, minutosDaDisciplinaNoDia(p, 1, "B"));
    }

    static void cenario07GerarDeNovoNaoDuplica() {
        titulo("Cenário 07 - gerar novamente não duplica sessões");
        PlanejadorEstudos p = cenarioBase();
        p.gerarPlanejamento();
        int antes = p.getSessoes().size();
        p.gerarPlanejamento();
        p.gerarPlanejamento();
        verificarIgual("quantidade de sessões", antes, p.getSessoes().size());
        ArrayList<SessaoEstudo> todas = p.getSessoes();
        boolean duplicada = false;
        for (int i = 0; i < todas.size(); i++) {
            for (int j = i + 1; j < todas.size(); j++) {
                if (todas.get(i).getDisciplina() == todas.get(j).getDisciplina()
                        && todas.get(i).getData().equals(todas.get(j).getData())) {
                    duplicada = true;
                }
            }
        }
        verificar("nenhum par disciplina+data repetido", !duplicada);
    }

    static void cenario08ValoresInvalidosSaoRejeitados() {
        titulo("Cenário 08 - dificuldade, horas e nomes inválidos");
        PlanejadorEstudos p = new PlanejadorEstudos(HOJE);
        esperarErro("dificuldade 0", () -> p.cadastrarDisciplina("X", 0));
        esperarErro("dificuldade 6", () -> p.cadastrarDisciplina("X", 6));
        esperarErro("dificuldade negativa", () -> p.cadastrarDisciplina("X", -2));
        esperarErro("nome vazio", () -> p.cadastrarDisciplina("   ", 3));
        p.cadastrarDisciplina("Redes", 3);
        esperarErro("nome repetido (sem diferenciar maiúsculas)", () -> p.cadastrarDisciplina("redes", 2));
        esperarErro("horas negativas", () -> p.definirDisponibilidadeHoras(-1));
        esperarErro("horas NaN", () -> p.definirDisponibilidadeHoras(Double.NaN));
        esperarErro("17 horas por dia", () -> p.definirDisponibilidadeHoras(17));
        esperarErro("0,25 hora (não múltiplo de 30 min)", () -> p.definirDisponibilidadeHoras(0.25));
        esperarErro("alterar dificuldade para 9",
                () -> p.alterarDificuldade(p.buscarDisciplina("Redes"), 9));
        verificarIgual("dificuldade de Redes continua 3", 3, p.buscarDisciplina("Redes").getDificuldade());
        esperarErro("gerar sem disciplinas", () -> new PlanejadorEstudos(HOJE).gerarPlanejamento());
    }

    static void cenario09DatasInvalidasSaoRejeitadas() {
        titulo("Cenário 09 - datas de avaliação inválidas");
        PlanejadorEstudos p = new PlanejadorEstudos(HOJE);
        Disciplina d = p.cadastrarDisciplina("Redes", 3);
        esperarErro("data no passado", () -> p.registrarAvaliacao(d, HOJE.minusDays(1), "P1"));
        esperarErro("data nula", () -> p.registrarAvaliacao(d, null, "P1"));
        esperarErro("data muito distante", () -> p.registrarAvaliacao(d, HOJE.plusDays(731), "P1"));
        p.registrarAvaliacao(d, HOJE.plusDays(10), "P1");
        esperarErro("duas avaliações da mesma disciplina no mesmo dia",
                () -> p.registrarAvaliacao(d, HOJE.plusDays(10), "P2"));
        try {
            java.time.LocalDate.parse("31/02/2026", PlanejadorEstudos.FORMATO_DATA);
            verificar("31/02/2026 deveria ser recusada", false);
        } catch (java.time.format.DateTimeParseException e) {
            verificar("31/02/2026 recusada pelo formato", true);
        }
    }

    static void cenario10RegerarPreservaConcluidas() {
        titulo("Cenário 10 - regerar preserva sessões concluídas");
        PlanejadorEstudos p = cenarioBase();
        p.gerarPlanejamento();
        SessaoEstudo calcDia1 = p.consultarDia(1).get(0); // Cálculo 2h
        p.marcarConcluida(calcDia1.getId());

        p.definirDisponibilidadeHoras(2); // concluída já usa as 2h do dia 1
        p.gerarPlanejamento();
        verificarIgual("dia 1 só tem a sessão concluída", 1, p.consultarDia(1).size());
        verificarIgual("dia 1 continua com 120 min", 120, minutosDoDia(p, 1));
        verificarIgual("dia 2 usa 2h", 120, minutosDoDia(p, 2));

        p.definirDisponibilidadeHoras(3); // sobra 1h no dia 1 para História e Programação
        p.gerarPlanejamento();
        verificarIgual("dia 1 total 3h", 180, minutosDoDia(p, 1));
        verificarIgual("Cálculo dia 1 não duplicou", 120, minutosDaDisciplinaNoDia(p, 1, "Cálculo"));
        // 2 blocos entre História (46) e Programação (30): 1,21 e 0,79 -> 1 e 1
        verificarIgual("História dia 1", 30, minutosDaDisciplinaNoDia(p, 1, "História"));
        verificarIgual("Programação dia 1", 30, minutosDaDisciplinaNoDia(p, 1, "Programação"));
    }

    static void cenario11ProgressoEHorasPorDisciplina() {
        titulo("Cenário 11 - progresso e horas por disciplina");
        PlanejadorEstudos p = cenarioBase();
        p.gerarPlanejamento();
        verificarIgual("total planejado 7 x 4h", 1680, p.minutosPlanejadosTotal());
        int somaDisciplinas = 0;
        for (Disciplina d : p.getDisciplinas()) {
            somaDisciplinas += p.minutosPlanejados(d);
        }
        verificarIgual("soma por disciplina = total", 1680, somaDisciplinas);
        verificarIgual("Cálculo: 3 dias x 2h = 360 min", 360, p.minutosPlanejados(p.buscarDisciplina("Cálculo")));
        verificarIgual("progresso inicial 0%", 0,
                PlanejadorEstudos.percentual(p.minutosConcluidosTotal(), p.minutosPlanejadosTotal()));
        for (SessaoEstudo s : p.consultarDia(1)) {
            p.marcarConcluida(s.getId());
        }
        verificarIgual("concluído 240 min", 240, p.minutosConcluidosTotal());
        // 240 x 100 / 1680 = 14,28 -> 14 (divisão inteira)
        verificarIgual("progresso 14%", 14,
                PlanejadorEstudos.percentual(p.minutosConcluidosTotal(), p.minutosPlanejadosTotal()));
        verificarIgual("percentual sem sessões", 0, PlanejadorEstudos.percentual(0, 0));
    }

    static void cenario12DisponibilidadeZero() {
        titulo("Cenário 12 - disponibilidade zero");
        PlanejadorEstudos p = cenarioBase();
        p.definirDisponibilidadeHoras(0);
        p.gerarPlanejamento();
        verificarIgual("nenhuma sessão", 0, p.getSessoes().size());
    }

    static void cenario13ProvaMaisProximaRecebeMais() {
        titulo("Cenário 13 - mesma dificuldade, prova mais próxima recebe mais tempo");
        PlanejadorEstudos p = new PlanejadorEstudos(HOJE);
        Disciplina perto = p.cadastrarDisciplina("Perto", 3);
        Disciplina longe = p.cadastrarDisciplina("Longe", 3);
        p.registrarAvaliacao(perto, HOJE.plusDays(2), "P1");
        p.registrarAvaliacao(longe, HOJE.plusDays(9), "P1");
        p.definirDisponibilidadeHoras(4);
        p.gerarPlanejamento();
        verificar("Perto > Longe no dia 1",
                minutosDaDisciplinaNoDia(p, 1, "Perto") > minutosDaDisciplinaNoDia(p, 1, "Longe"));
        // Dificuldade maior com a mesma proximidade também recebe mais
        PlanejadorEstudos q = new PlanejadorEstudos(HOJE);
        Disciplina dificil = q.cadastrarDisciplina("Difícil", 5);
        Disciplina facil = q.cadastrarDisciplina("Fácil", 1);
        q.registrarAvaliacao(dificil, HOJE.plusDays(5), "P1");
        q.registrarAvaliacao(facil, HOJE.plusDays(5), "P1");
        q.definirDisponibilidadeHoras(4);
        q.gerarPlanejamento();
        verificar("Difícil > Fácil no dia 1",
                minutosDaDisciplinaNoDia(q, 1, "Difícil") > minutosDaDisciplinaNoDia(q, 1, "Fácil"));
    }

    static void cenario14DisciplinaSemAvaliacao() {
        titulo("Cenário 14 - disciplina sem avaliação registrada");
        PlanejadorEstudos p = new PlanejadorEstudos(HOJE);
        Disciplina livre = p.cadastrarDisciplina("Leitura", 2);
        verificarIgual("proximidade mínima (1)", 1, p.calcularPontosProximidade(livre, HOJE));
        verificarIgual("pontuação 4 x 4 + 6 x 1", 22, p.calcularPontuacao(livre, HOJE));
        p.definirDisponibilidadeHoras(2);
        p.gerarPlanejamento();
        for (int dia = 1; dia <= 7; dia++) {
            verificarIgual("Leitura dia " + dia, 120, minutosDaDisciplinaNoDia(p, dia, "Leitura"));
        }
    }

    static void cenario15ConclusaoInvalida() {
        titulo("Cenário 15 - marcar sessão inexistente ou já concluída");
        PlanejadorEstudos p = cenarioBase();
        p.gerarPlanejamento();
        esperarErro("sessão inexistente", () -> p.marcarConcluida(9999));
        int id = p.consultarDia(1).get(0).getId();
        p.marcarConcluida(id);
        esperarErro("concluir duas vezes", () -> p.marcarConcluida(id));
        esperarErro("dia 0", () -> p.consultarDia(0));
        esperarErro("dia 8", () -> p.consultarDia(8));
    }
}
