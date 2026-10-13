package com.gestao.agro.model;

import java.time.LocalDate;
import java.util.Objects;

public class ItemGut {

    private Integer id;
    private Integer organizacaoId;
    private Integer produtorId;
    private String problemaAcao;      // Descrição do problema ou da ação a ser priorizada
    private String origem;            // IMO-AF, PESTEL, FOFA, TOWS, MANUAL
    private int gravidade;            // 1 a 5
    private int urgencia;             // 1 a 5
    private int tendencia;            // 1 a 5
    private int scoreGut;             // G * U * T (1 a 125)
    private String nivelPrioridade;   // CRÍTICO, ALTO, MÉDIO, BAIXO
    private String acaoProposta;      // Ação inicial recomendada antes de ir para o 5W2H
    private String status;            // PENDENTE, EM_ANDAMENTO, CONCLUIDO
    private LocalDate dataRegistro;

    public ItemGut() {
        this.gravidade = 3;
        this.urgencia = 3;
        this.tendencia = 3;
        this.calcularScore();
        this.origem = "MANUAL";
        this.status = "PENDENTE";
        this.dataRegistro = LocalDate.now();
    }

    public ItemGut(Integer organizacaoId, Integer produtorId, String problemaAcao, 
                   String origem, int gravidade, int urgencia, int tendencia, String acaoProposta) {
        this.organizacaoId = organizacaoId;
        this.produtorId = produtorId;
        this.problemaAcao = problemaAcao;
        this.origem = origem;
        this.gravidade = gravidade;
        this.urgencia = urgencia;
        this.tendencia = tendencia;
        this.acaoProposta = acaoProposta;
        this.status = "PENDENTE";
        this.dataRegistro = LocalDate.now();
        this.calcularScore();
    }

    public void calcularScore() {
        this.scoreGut = this.gravidade * this.urgencia * this.tendencia;
        if (this.scoreGut >= 75) {
            this.nivelPrioridade = "CRÍTICO";
        } else if (this.scoreGut >= 45) {
            this.nivelPrioridade = "ALTO";
        } else if (this.scoreGut >= 20) {
            this.nivelPrioridade = "MÉDIO";
        } else {
            this.nivelPrioridade = "BAIXO";
        }
    }

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrganizacaoId() { return organizacaoId; }
    public void setOrganizacaoId(Integer organizacaoId) { this.organizacaoId = organizacaoId; }

    public Integer getProdutorId() { return produtorId; }
    public void setProdutorId(Integer produtorId) { this.produtorId = produtorId; }

    public String getProblemaAcao() { return problemaAcao; }
    public void setProblemaAcao(String problemaAcao) { this.problemaAcao = problemaAcao; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public int getGravidade() { return gravidade; }
    public void setGravidade(int gravidade) { 
        this.gravidade = gravidade; 
        this.calcularScore();
    }

    public int getUrgencia() { return urgencia; }
    public void setUrgencia(int urgencia) { 
        this.urgencia = urgencia; 
        this.calcularScore();
    }

    public int getTendencia() { return tendencia; }
    public void setTendencia(int tendencia) { 
        this.tendencia = tendencia; 
        this.calcularScore();
    }

    public int getScoreGut() { return scoreGut; }
    public void setScoreGut(int scoreGut) { this.scoreGut = scoreGut; }

    public String getNivelPrioridade() { return nivelPrioridade; }
    public void setNivelPrioridade(String nivelPrioridade) { this.nivelPrioridade = nivelPrioridade; }

    public String getAcaoProposta() { return acaoProposta; }
    public void setAcaoProposta(String acaoProposta) { this.acaoProposta = acaoProposta; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(LocalDate dataRegistro) { this.dataRegistro = dataRegistro; }

    // equals e hashCode garantem estabilidade nas operações do TableView
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemGut itemGut = (ItemGut) o;
        return Objects.equals(id, itemGut.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "[" + scoreGut + " pts - " + nivelPrioridade + "] " + problemaAcao;
    }
}