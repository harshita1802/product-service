package dev.harshita.EcomProductService.EcomProductService.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Rating extends BaseModel{
    private double rating;
    private int count;
}
