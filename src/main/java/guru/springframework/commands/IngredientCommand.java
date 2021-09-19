package guru.springframework.commands;

import java.math.BigDecimal;

public class IngredientCommand {
    private Long id;
    private String description;
    private BigDecimal amount;
    private UnitOfMeasureCommand unitOfMeasure;

    public IngredientCommand() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public UnitOfMeasureCommand getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasureCommand unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
}
