import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Menu de terminal do StudyFlow. Uso: java -cp out Main [aaaa-mm-dd] */
public class Main {
    private static final Scanner entrada = new Scanner(System.in);
    private static PlanejadorEstudos planejador;

    public static void main(String[] args) {
        LocalDate hoje = LocalDate.now();
        if (args.length > 0) {
            try {
                hoje = LocalDate.parse(args[0]); // formato aaaa-mm-dd
            } catch (DateTimeParseException e) {
                System.out.println("Data inicial inválida. Use aaaa-mm-dd. Usando a data de hoje.");
            }
        }
        planejador = new PlanejadorEstudos(hoje);

        System.out.println("=== StudyFlow - Organizador e Planejador de Estudos ===");
        System.out.println("Data de referência: " + formatarDataComSemana(hoje));

        boolean rodando = true;
        while (rodando) {
            try {
                mostrarMenu();
                int opcao = lerInteiro("Escolha: ");
                switch (opcao) {
                    case 1 -> cadastrarDisciplina();
                    case 2 -> registrarAvaliacao();
                    case 3 -> alterarDificuldade();
                    case 4 -> alterarDisponibilidade();
                    case 5 -> mostrarPrioridades();
                    case 6 -> gerarPlanejamento();
                    case 7 -> consultarDia();
                    case 8 -> marcarConcluida();
                    case 9 -> mostrarProgresso();
                    case 10 -> mostrarHorasPorDisciplina();
                    case 11 -> carregarDemonstracao();
                    case 0 -> rodando = false;
                    default -> System.out.println("Opção inválida.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Erro: " + e.getMessage());
            } catch (NoSuchElementException e) {
                rodando = false; // acabou a entrada de dados
            }
        }
        System.out.println("Até a próxima sessão de estudos!");
    }

    private static void mostrarMenu() {
        System.out.println();
        System.out.println("1  Cadastrar disciplina");
        System.out.println("2  Registrar data de avaliação");
        System.out.println("3  Alterar dificuldade (1 a 5)");
        System.out.println("4  Definir/alterar horas disponíveis por dia");
        System.out.println("5  Ver prioridades de hoje");
        System.out.println("6  Gerar planejamento de 7 dias");
        System.out.println("7  Consultar um dia do planejamento");
        System.out.println("8  Marcar sessão como concluída");
        System.out.println("9  Ver progresso");
        System.out.println("10 Ver horas por disciplina");
        System.out.println("11 Carregar exemplo de demonstração");
        System.out.println("0  Sair");
    }

    // ---------------- ações do menu ----------------

    private static void cadastrarDisciplina() {
        String nome = lerTexto("Nome da disciplina: ");
        int dificuldade = lerInteiro("Dificuldade (1 a 5): ");
        planejador.cadastrarDisciplina(nome, dificuldade);
        System.out.println("Disciplina cadastrada.");
    }

    private static void registrarAvaliacao() {
        Disciplina d = escolherDisciplina();
        LocalDate data = lerData("Data da avaliação (dd/MM/aaaa): ");
        String descricao = lerTexto("Descrição (ex.: Prova 1): ");
        Avaliacao a = planejador.registrarAvaliacao(d, data, descricao);
        System.out.println("Avaliação registrada: " + a);
        if (data.equals(planejador.getHoje())) {
            System.out.println("Atenção: a avaliação é hoje. Nenhuma sessão será criada para ela "
                    + "(não se planeja estudo no dia da prova).");
        }
    }

    private static void alterarDificuldade() {
        Disciplina d = escolherDisciplina();
        int nova = lerInteiro("Nova dificuldade (1 a 5): ");
        planejador.alterarDificuldade(d, nova);
        System.out.println("Dificuldade alterada. Gere o planejamento novamente (opção 6).");
    }

    private static void alterarDisponibilidade() {
        double horas = lerHoras("Horas disponíveis por dia (ex.: 2 ou 2,5): ");
        planejador.definirDisponibilidadeHoras(horas);
        System.out.println("Disponibilidade: "
                + PlanejadorEstudos.formatarMinutos(planejador.getDisponibilidadeMinutos()) + " por dia.");
        if (!planejador.getSessoes().isEmpty() && !planejador.getDisciplinas().isEmpty()) {
            planejador.gerarPlanejamento();
            System.out.println("Planejamento gerado novamente com a nova disponibilidade.");
        }
    }

    private static void mostrarPrioridades() {
        LocalDate hoje = planejador.getHoje();
        ArrayList<Disciplina> ordem = planejador.ordenarPorPrioridade(hoje);
        if (ordem.isEmpty()) {
            System.out.println("Nenhuma disciplina elegível hoje.");
            return;
        }
        System.out.println("Prioridades em " + formatarDataComSemana(hoje) + ":");
        System.out.println("pontuação = " + PlanejadorEstudos.PESO_DIFICULDADE + " x pontosDificuldade + "
                + PlanejadorEstudos.PESO_PROXIMIDADE + " x pontosProximidade");
        int posicao = 1;
        for (Disciplina d : ordem) {
            int pd = planejador.calcularPontosDificuldade(d);
            int pp = planejador.calcularPontosProximidade(d, hoje);
            int total = planejador.calcularPontuacao(d, hoje);
            Avaliacao prox = d.proximaAvaliacaoApos(hoje);
            String quando = (prox == null) ? "sem avaliação futura" : prox.toString();
            System.out.println(posicao + ". " + d.getNome() + " | dificuldade " + pd + " pts"
                    + " | proximidade " + pp + " pts | TOTAL " + total + " | " + quando);
            posicao++;
        }
    }

    private static void gerarPlanejamento() {
        if (planejador.getDisponibilidadeMinutos() == 0) {
            System.out.println("Aviso: a disponibilidade diária é 0h. Use a opção 4 para defini-la.");
        }
        planejador.gerarPlanejamento();
        System.out.println("Planejamento de 7 dias gerado.");
        for (int dia = 1; dia <= PlanejadorEstudos.DIAS_PLANEJAMENTO; dia++) {
            mostrarDia(dia);
        }
        avisarDisciplinasSemSessao();
    }

    private static void consultarDia() {
        avisarSeDesatualizado();
        int dia = lerInteiro("Qual dia? (1 a 7, ou 0 para todos): ");
        if (dia == 0) {
            for (int i = 1; i <= PlanejadorEstudos.DIAS_PLANEJAMENTO; i++) {
                mostrarDia(i);
            }
        } else {
            mostrarDia(dia);
        }
    }

    private static void mostrarDia(int numeroDia) {
        LocalDate data = planejador.dataDoDia(numeroDia);
        ArrayList<SessaoEstudo> doDia = planejador.consultarDia(numeroDia);
        int total = 0;
        System.out.println("Dia " + numeroDia + " - " + formatarDataComSemana(data));
        if (doDia.isEmpty()) {
            System.out.println("   (sem sessões)");
        }
        for (SessaoEstudo s : doDia) {
            System.out.println("   " + s);
            total += s.getMinutos();
        }
        System.out.println("   Total do dia: " + PlanejadorEstudos.formatarMinutos(total)
                + " de " + PlanejadorEstudos.formatarMinutos(planejador.getDisponibilidadeMinutos()));
    }

    private static void marcarConcluida() {
        ArrayList<SessaoEstudo> pendentes = new ArrayList<>();
        for (SessaoEstudo s : planejador.getSessoes()) {
            if (!s.isConcluida()) {
                pendentes.add(s);
            }
        }
        if (pendentes.isEmpty()) {
            System.out.println("Não há sessões pendentes.");
            return;
        }
        System.out.println("Sessões pendentes:");
        for (SessaoEstudo s : pendentes) {
            System.out.println("   " + s + " em " + s.getData().format(PlanejadorEstudos.FORMATO_DATA));
        }
        int id = lerInteiro("Número da sessão concluída: ");
        planejador.marcarConcluida(id);
        System.out.println("Sessão marcada como concluída.");
    }

    private static void mostrarProgresso() {
        int planejado = planejador.minutosPlanejadosTotal();
        int concluido = planejador.minutosConcluidosTotal();
        System.out.println("Progresso geral: " + PlanejadorEstudos.formatarMinutos(concluido) + " de "
                + PlanejadorEstudos.formatarMinutos(planejado) + " ("
                + PlanejadorEstudos.percentual(concluido, planejado) + "%)");
        for (Disciplina d : planejador.getDisciplinas()) {
            int p = planejador.minutosPlanejados(d);
            int c = planejador.minutosConcluidos(d);
            System.out.println("   " + d.getNome() + ": " + PlanejadorEstudos.formatarMinutos(c) + " de "
                    + PlanejadorEstudos.formatarMinutos(p) + " (" + PlanejadorEstudos.percentual(c, p) + "%)");
        }
    }

    private static void mostrarHorasPorDisciplina() {
        avisarSeDesatualizado();
        System.out.println("Horas atribuídas por disciplina:");
        for (Disciplina d : planejador.getDisciplinas()) {
            System.out.println("   " + d.getNome() + ": "
                    + PlanejadorEstudos.formatarMinutos(planejador.minutosPlanejados(d)));
        }
        System.out.println("   TOTAL: " + PlanejadorEstudos.formatarMinutos(planejador.minutosPlanejadosTotal()));
    }

    private static void carregarDemonstracao() {
        if (!planejador.getDisciplinas().isEmpty()) {
            System.out.println("A demonstração só pode ser carregada sem disciplinas cadastradas.");
            return;
        }
        LocalDate hoje = planejador.getHoje();
        Disciplina calculo = planejador.cadastrarDisciplina("Cálculo", 5);
        Disciplina historia = planejador.cadastrarDisciplina("História", 2);
        Disciplina programacao = planejador.cadastrarDisciplina("Programação", 3);
        planejador.registrarAvaliacao(calculo, hoje.plusDays(3), "Prova 1");
        planejador.registrarAvaliacao(historia, hoje.plusDays(6), "Prova 1");
        planejador.registrarAvaliacao(programacao, hoje.plusDays(12), "Prova 1");
        planejador.definirDisponibilidadeHoras(4);
        System.out.println("Demonstração carregada: Cálculo (dif. 5, prova em 3 dias), "
                + "História (dif. 2, em 6 dias), Programação (dif. 3, em 12 dias), 4h por dia.");
        System.out.println("Use a opção 5 para ver as prioridades e a 6 para gerar o plano.");
    }

    // ---------------- avisos ----------------

    private static void avisarSeDesatualizado() {
        if (planejador.isDesatualizado()) {
            System.out.println("Aviso: houve mudanças desde a última geração. "
                    + "Use a opção 6 para atualizar o planejamento.");
        }
    }

    private static void avisarDisciplinasSemSessao() {
        for (Disciplina d : planejador.getDisciplinas()) {
            if (planejador.minutosPlanejados(d) == 0) {
                System.out.println("Aviso: " + d.getNome() + " não recebeu sessões "
                        + "(avaliação hoje/já passada ou sem horas disponíveis).");
            }
        }
    }

    // ---------------- leitura de dados ----------------

    private static Disciplina escolherDisciplina() {
        ArrayList<Disciplina> lista = planejador.getDisciplinas();
        if (lista.isEmpty()) {
            throw new IllegalStateException("Cadastre uma disciplina primeiro.");
        }
        for (int i = 0; i < lista.size(); i++) {
            System.out.println((i + 1) + ". " + lista.get(i));
        }
        int numero = lerInteiro("Número da disciplina: ");
        if (numero < 1 || numero > lista.size()) {
            throw new IllegalArgumentException("Disciplina inexistente.");
        }
        return lista.get(numero - 1);
    }

    private static String lerTexto(String mensagem) {
        System.out.print(mensagem);
        return entrada.nextLine().trim(); // lança NoSuchElementException se acabar a entrada
    }

    private static int lerInteiro(String mensagem) {
        String texto = lerTexto(mensagem);
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Digite um número inteiro.");
        }
    }

    private static double lerHoras(String mensagem) {
        String texto = lerTexto(mensagem).replace(',', '.');
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Digite um número, como 2 ou 2,5.");
        }
    }

    private static LocalDate lerData(String mensagem) {
        String texto = lerTexto(mensagem);
        try {
            return LocalDate.parse(texto, PlanejadorEstudos.FORMATO_DATA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida. Use dd/MM/aaaa (ex.: 25/11/2026).");
        }
    }

    private static String formatarDataComSemana(LocalDate data) {
        String[] nomes = {"seg", "ter", "qua", "qui", "sex", "sáb", "dom"};
        DayOfWeek dia = data.getDayOfWeek();
        return nomes[dia.getValue() - 1] + " " + data.format(PlanejadorEstudos.FORMATO_DATA);
    }
}