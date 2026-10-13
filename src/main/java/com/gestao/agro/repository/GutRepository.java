package com.gestao.agro.repository;

import com.gestao.agro.model.CruzamentoSwot;
import com.gestao.agro.model.FatorSwot;
import com.gestao.agro.model.ItemGut;
import com.gestao.agro.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GutRepository {

    public GutRepository() {
        criarTabelaSeNaoExistir();
    }

    private void criarTabelaSeNaoExistir() {
        String sql = """
            CREATE TABLE IF NOT EXISTS matriz_gut (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                organizacao_id INTEGER,
                produtor_id INTEGER,
                problema_acao TEXT NOT NULL,
                origem TEXT DEFAULT 'MANUAL',
                gravidade INTEGER DEFAULT 3,
                urgencia INTEGER DEFAULT 3,
                tendencia INTEGER DEFAULT 3,
                score_gut INTEGER DEFAULT 27,
                nivel_prioridade TEXT DEFAULT 'MÉDIO',
                acao_proposta TEXT,
                status TEXT DEFAULT 'PENDENTE',
                data_registro TEXT,
                FOREIGN KEY (organizacao_id) REFERENCES organizacao(id),
                FOREIGN KEY (produtor_id) REFERENCES produtor(id)
            );
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar tabela Matriz GUT: " + e.getMessage());
        }
    }

    // ==========================================
    //            OPERAÇÕES CRUD BÁSICAS
    // ==========================================

    public ItemGut salvar(ItemGut item) throws SQLException {
        item.calcularScore();
        String sql = """
            INSERT INTO matriz_gut (
                organizacao_id, produtor_id, problema_acao, origem, gravidade, urgencia, 
                tendencia, score_gut, nivel_prioridade, acao_proposta, status, data_registro
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (item.getOrganizacaoId() != null) {
                stmt.setInt(1, item.getOrganizacaoId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }

            if (item.getProdutorId() != null) {
                stmt.setInt(2, item.getProdutorId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setString(3, item.getProblemaAcao());
            stmt.setString(4, item.getOrigem() != null ? item.getOrigem() : "MANUAL");
            stmt.setInt(5, item.getGravidade());
            stmt.setInt(6, item.getUrgencia());
            stmt.setInt(7, item.getTendencia());
            stmt.setInt(8, item.getScoreGut());
            stmt.setString(9, item.getNivelPrioridade());
            stmt.setString(10, item.getAcaoProposta());
            stmt.setString(11, item.getStatus() != null ? item.getStatus() : "PENDENTE");
            stmt.setString(12, item.getDataRegistro() != null ? item.getDataRegistro().toString() : LocalDate.now().toString());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getInt(1));
                }
            }

            if (item.getId() == null || item.getId() == 0) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        item.setId(rs.getInt(1));
                    }
                }
            }
        }
        return item;
    }

    public void atualizar(ItemGut item) throws SQLException {
        item.calcularScore();
        String sql = """
            UPDATE matriz_gut SET
                problema_acao = ?,
                origem = ?,
                gravidade = ?,
                urgencia = ?,
                tendencia = ?,
                score_gut = ?,
                nivel_prioridade = ?,
                acao_proposta = ?,
                status = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getProblemaAcao());
            stmt.setString(2, item.getOrigem());
            stmt.setInt(3, item.getGravidade());
            stmt.setInt(4, item.getUrgencia());
            stmt.setInt(5, item.getTendencia());
            stmt.setInt(6, item.getScoreGut());
            stmt.setString(7, item.getNivelPrioridade());
            stmt.setString(8, item.getAcaoProposta());
            stmt.setString(9, item.getStatus());
            stmt.setInt(10, item.getId());

            stmt.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM matriz_gut WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public List<ItemGut> listarPorOrganizacao(int orgId) throws SQLException {
        List<ItemGut> lista = new ArrayList<>();
        String sql = "SELECT * FROM matriz_gut WHERE organizacao_id = ? ORDER BY score_gut DESC, gravidade DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(extrairItemGut(rs));
                }
            }
        }
        return lista;
    }

    public List<ItemGut> listarPorProdutor(int prodId) throws SQLException {
        List<ItemGut> lista = new ArrayList<>();
        String sql = "SELECT * FROM matriz_gut WHERE produtor_id = ? ORDER BY score_gut DESC, gravidade DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, prodId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(extrairItemGut(rs));
                }
            }
        }
        return lista;
    }

    // ==========================================
    //     IMPORTAÇÃO AUTOMATIZADA: FOFA / TOWS
    // ==========================================

    public int importarFraquezasAmeacasSwot(Integer orgId, Integer prodId) throws SQLException {
        SwotRepository swotRepo = new SwotRepository();
        List<FatorSwot> fatores = (orgId != null) 
            ? swotRepo.listarFatoresPorOrganizacao(orgId) 
            : swotRepo.listarFatoresPorProdutor(prodId);

        List<ItemGut> itensExistentes = (orgId != null) 
            ? listarPorOrganizacao(orgId) 
            : listarPorProdutor(prodId);

        int totalImportados = 0;

        for (FatorSwot f : fatores) {
            if (f.getTipo() == null) continue;
            String tipo = f.getTipo().toUpperCase();

            // Prioriza fraquezas e ameaças
            if (tipo.contains("FRAQ") || tipo.contains("AMEA")) {
                String rotulo = tipo.contains("FRAQ") ? "[Fraqueza] " : "[Ameaça] ";
                String textoCompleto = rotulo + f.getDescricao();

                boolean jaExiste = itensExistentes.stream().anyMatch(i -> 
                    i.getProblemaAcao() != null && i.getProblemaAcao().equalsIgnoreCase(textoCompleto)
                );

                if (!jaExiste) {
                    ItemGut novo = new ItemGut();
                    novo.setOrganizacaoId(orgId);
                    novo.setProdutorId(prodId);
                    novo.setProblemaAcao(textoCompleto);
                    novo.setOrigem("FOFA");
                    novo.setGravidade(f.getIntensidade());
                    novo.setUrgencia(3);
                    novo.setTendencia(3);
                    novo.setAcaoProposta("Definir ação de contenção/mitigação");
                    salvar(novo);
                    totalImportados++;
                }
            }
        }
        return totalImportados;
    }

    public int importarEstrategiasTows(Integer orgId, Integer prodId) throws SQLException {
        SwotRepository swotRepo = new SwotRepository();
        List<CruzamentoSwot> cruzamentos = (orgId != null) 
            ? swotRepo.listarCruzamentosPorOrganizacao(orgId) 
            : swotRepo.listarCruzamentosPorProdutor(prodId);

        List<ItemGut> itensExistentes = (orgId != null) 
            ? listarPorOrganizacao(orgId) 
            : listarPorProdutor(prodId);

        int totalImportados = 0;

        for (CruzamentoSwot c : cruzamentos) {
            String textoCompleto = "[Estratégia " + c.getTipoEstrategia() + "] " + c.getEstrategiaProposta();

            boolean jaExiste = itensExistentes.stream().anyMatch(i -> 
                i.getProblemaAcao() != null && i.getProblemaAcao().equalsIgnoreCase(textoCompleto)
            );

            if (!jaExiste) {
                ItemGut novo = new ItemGut();
                novo.setOrganizacaoId(orgId);
                novo.setProdutorId(prodId);
                novo.setProblemaAcao(textoCompleto);
                novo.setOrigem("TOWS");
                novo.setGravidade(4);
                novo.setUrgencia(3);
                novo.setTendencia(3);
                novo.setAcaoProposta(c.getEstrategiaProposta());
                salvar(novo);
                totalImportados++;
            }
        }
        return totalImportados;
    }

    // ==========================================
    //            MÉTODO AUXILIAR
    // ==========================================

    private ItemGut extrairItemGut(ResultSet rs) throws SQLException {
        ItemGut item = new ItemGut();
        item.setId(rs.getInt("id"));

        int orgId = rs.getInt("organizacao_id");
        item.setOrganizacaoId(rs.wasNull() ? null : orgId);

        int prodId = rs.getInt("produtor_id");
        item.setProdutorId(rs.wasNull() ? null : prodId);

        item.setProblemaAcao(rs.getString("problema_acao"));
        item.setOrigem(rs.getString("origem"));
        item.setGravidade(rs.getInt("gravidade"));
        item.setUrgencia(rs.getInt("urgencia"));
        item.setTendencia(rs.getInt("tendencia"));
        item.setScoreGut(rs.getInt("score_gut"));
        item.setNivelPrioridade(rs.getString("nivel_prioridade"));
        item.setAcaoProposta(rs.getString("acao_proposta"));
        item.setStatus(rs.getString("status"));

        String dtStr = rs.getString("data_registro");
        if (dtStr != null && !dtStr.isBlank()) {
            try {
                item.setDataRegistro(LocalDate.parse(dtStr));
            } catch (Exception ignored) {}
        }

        return item;
    }
}