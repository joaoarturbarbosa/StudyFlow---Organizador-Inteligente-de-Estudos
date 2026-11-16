import java.time.LocalDate;

/**
 * Um bloco de estudo de uma disciplina em um dia. A duração é guardada em
 * minutos (número inteiro) para evitar erros de arredondamento com decimais.
 */
public class SessaoEstudo {
    private final int id;
    private final Disciplina disciplina;
    private final LocalDate data;
    private final int minutos;
    private boolean concluida;

    public SessaoEstudo(int id, Disciplina disciplina, LocalDate data, int minutos) {
        if (disciplina == null || data == null) {
            throw new IllegalArgumentException("Disciplina e data são obrigatórias.");
        }
        if (minutos <= 0) {
            throw new IllegalArgumentException("A duração da sessão deve ser positiva.");
        }
        this.id = id;
        this.disciplina = disciplina;
        this.data = data;
        this.minutos = minutos;
        this.concluida = false;
    }

    public int getId() {
        return id;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

    public LocalDate getData() {
        return data;
    }

    public int getMinutos() {
        return minutos;
    }

    public boolean isConcluida() {
        return concluida;
    }

    public void concluir() {
        if (concluida) {
            throw new IllegalStateException("A sessão " + id + " já está concluída.");
        }
        concluida = true;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + (concluida ? "[x] " : "[ ] ")
                + disciplina.getNome() + " - " + PlanejadorEstudos.formatarMinutos(minutos);
    }
}
