import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Uma disciplina com nome, dificuldade (1 a 5) e suas avaliações.
 */
public class Disciplina {
    private final String nome;
    private int dificuldade;
    private final ArrayList<Avaliacao> avaliacoes = new ArrayList<>();

    public Disciplina(String nome, int dificuldade) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da disciplina não pode ser vazio.");
        }
        this.nome = nome.trim();
        setDificuldade(dificuldade); // reaproveita a validação
    }

    public String getNome() {
        return nome;
    }

    public int getDificuldade() {
        return dificuldade;
    }

    public void setDificuldade(int dificuldade) {
        if (dificuldade < 1 || dificuldade > 5) {
            throw new IllegalArgumentException("A dificuldade deve estar entre 1 e 5.");
        }
        this.dificuldade = dificuldade;
    }

    public void adicionarAvaliacao(Avaliacao avaliacao) {
        if (avaliacao == null) {
            throw new IllegalArgumentException("A avaliação não pode ser vazia.");
        }
        for (Avaliacao existente : avaliacoes) {
            if (existente.getData().equals(avaliacao.getData())) {
                throw new IllegalArgumentException(
                        nome + " já tem uma avaliação nessa data.");
            }
        }
        avaliacoes.add(avaliacao);
    }

    /** Devolve uma cópia da lista, para ninguém alterar a original por fora. */
    public ArrayList<Avaliacao> getAvaliacoes() {
        return new ArrayList<>(avaliacoes);
    }

    public boolean temAvaliacoes() {
        return !avaliacoes.isEmpty();
    }

    /**
     * Primeira avaliação que acontece DEPOIS do dia informado (estritamente depois).
     * Se não houver nenhuma, devolve null.
     */
    public Avaliacao proximaAvaliacaoApos(LocalDate dia) {
        Avaliacao escolhida = null;
        for (Avaliacao a : avaliacoes) {
            if (a.getData().isAfter(dia)) {
                if (escolhida == null || a.getData().isBefore(escolhida.getData())) {
                    escolhida = a;
                }
            }
        }
        return escolhida;
    }

    @Override
    public String toString() {
        return nome + " (dificuldade " + dificuldade + ")";
    }
}
