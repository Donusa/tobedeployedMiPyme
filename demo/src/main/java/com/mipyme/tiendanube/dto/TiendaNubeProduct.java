package com.mipyme.tiendanube.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TiendaNubeProduct {
    private Long id;
    private Map<String, String> name;

    @JsonProperty("variants")
    private List<TiendaNubeVariant> variants;

    @JsonProperty("images")
    private List<TiendaNubeImage> images;

    private Boolean published;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
    }

    public List<TiendaNubeVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<TiendaNubeVariant> variants) {
        this.variants = variants;
    }

    public List<TiendaNubeImage> getImages() {
        return images;
    }

    public void setImages(List<TiendaNubeImage> images) {
        this.images = images;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }
}
