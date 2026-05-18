package donatrack.model.contacto;

public class MedioContacto {

    private TipoContacto tipo;
    private String valor;

    public MedioContacto(TipoContacto tipo, String valor) {
        this.tipo = tipo;
        this.valor = valor;
    }

    public TipoContacto getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
