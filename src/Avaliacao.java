import java.time.LocalDate;

/**
 * Uma avaliação (prova, trabalho, seminário...) com descrição e data.
 * LocalDate guarda só a data (ano, mês, dia), sem hora.
 */
public class Avaliacao {
    private final String descricao;
    private final LocalDate data;

    public Avaliacao(String descricao, LocalDate data) {
        if (data == null) {
            throw new IllegalArgumentException("A data da avaliação não pode ser vazia.");
        }
        if (descricao == null || descricao.isBlank()) {
            this.descricao = "Prova";
        } else {
            this.descricao = descricao.trim();
        }
        this.data = data;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getData() {
        return data;
    }

    @Override
    public String toString() {
        return descricao + " (" + data.format(PlanejadorEstudos.FORMATO_DATA) + ")";
    }
}