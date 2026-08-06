package es.aelb.backendweb.domain.shared.valueobject;

/**
 * Marca un objeto como raíz de agregado.
 * ID tipado para garantizar que no se mezclan identidades entre agregados.
 */
public abstract class AggregateRoot<ID> {

    private final ID id;

    protected AggregateRoot(ID id) {
        if (id == null) throw new IllegalArgumentException("El ID del agregado no puede ser nulo");
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateRoot<?> that = (AggregateRoot<?>) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
