package com.gestao.agro.controller;

import com.gestao.agro.model.ItemGut;
import com.gestao.agro.model.Organizacao;
import com.gestao.agro.model.Produtor;
import com.gestao.agro.repository.GutRepository;
import com.gestao.agro.repository.OrganizacaoRepository;
import com.gestao.agro.repository.ProdutorRepository;
import com.gestao.agro.util.SessaoUsuario;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.List;

public class GutController {

    // --- Seleção de Entidade ---
    @FXML private RadioButton rbTipoOrg;
    @FXML private RadioButton rbTipoProd;
    @FXML private ToggleGroup grpTipoAlvo;
    @FXML private Label lblEntidadeAlvo;
    @FXML private ComboBox<Object> cbEntidadeAlvo;
    @FXML private TextField txtConsultor;

    // --- Contadores de Criticidade ---
    @FXML private Label lblCountCritico;
    @FXML private Label lblCountAlto;
    @FXML private Label lblCountMedio;
    @FXML private Label lblCountBaixo;

    // --- Formulário GUT ---
    @FXML private TextField txtProblemaAcao;
    @FXML private Slider sldGravidade;
    @FXML private Slider sldUrgencia;
    @FXML private Slider sldTendencia;
    @FXML private Label lblValorGravidade;
    @FXML private Label lblValorUrgencia;
    @FXML private Label lblValorTendencia;
    @FXML private Label lblScoreCalculado;
    @FXML private Label lblPrioridadeCalculada;
    @FXML private TextField txtAcaoProposta;
    @FXML private ComboBox<String> cbStatus;

    // --- Tabela GUT ---
    @FXML private TableView<ItemGut> tblGut;
    @FXML private TableColumn<ItemGut, String> colProblema;
    @FXML private TableColumn<ItemGut, String> colOrigem;
    @FXML private TableColumn<ItemGut, Number> colGravidade;
    @FXML private TableColumn<ItemGut, Number> colUrgencia;
    @FXML private TableColumn<ItemGut, Number> colTendencia;
    @FXML private TableColumn<ItemGut, Number> colScore;
    @FXML private TableColumn<ItemGut, String> colPrioridade;
    @FXML private TableColumn<ItemGut, String> colAcao;
    @FXML private TableColumn<ItemGut, String> colStatus;

    @FXML private Label lblStatusMensagem;

    private final GutRepository gutRepo = new GutRepository();
    private final OrganizacaoRepository orgRepo = new OrganizacaoRepository();
    private final ProdutorRepository prodRepo = new ProdutorRepository();

    private final ObservableList<ItemGut> listaItensGut = FXCollections.observableArrayList();
    private ItemGut itemEmEdicao = null;
    private boolean ignorarListener = false;

    @FXML
    public void initialize() {
        txtConsultor.setText(SessaoUsuario.getNomeConsultor());

        configurarFormulario();
        configurarSeletoresEntidade();
        configurarTabela();

        alternarTipoAlvo();
    }

    private void configurarFormulario() {
        cbStatus.setItems(FXCollections.observableArrayList("PENDENTE", "EM ANDAMENTO", "CONCLUÍDO"));
        cbStatus.getSelectionModel().selectFirst();

        // Listeners para atualização imediata do score ao deslocar os sliders
        sldGravidade.valueProperty().addListener((obs, antigo, novo) -> {
            lblValorGravidade.setText(String.valueOf(novo.intValue()));
            recalcularPreviewScore();
        });

        sldUrgencia.valueProperty().addListener((obs, antigo, novo) -> {
            lblValorUrgencia.setText(String.valueOf(novo.intValue()));
            recalcularPreviewScore();
        });

        sldTendencia.valueProperty().addListener((obs, antigo, novo) -> {
            lblValorTendencia.setText(String.valueOf(novo.intValue()));
            recalcularPreviewScore();
        });

        recalcularPreviewScore();
    }

    private void recalcularPreviewScore() {
        int g = (int) sldGravidade.getValue();
        int u = (int) sldUrgencia.getValue();
        int t = (int) sldTendencia.getValue();
        int score = g * u * t;

        lblScoreCalculado.setText("Score: " + score);

        if (score >= 75) {
            lblPrioridadeCalculada.setText("CRÍTICO");
            lblPrioridadeCalculada.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #B71C1C; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold;");
        } else if (score >= 45) {
            lblPrioridadeCalculada.setText("ALTO");
            lblPrioridadeCalculada.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold;");
        } else if (score >= 20) {
            lblPrioridadeCalculada.setText("MÉDIO");
            lblPrioridadeCalculada.setStyle("-fx-background-color: #FFFDE7; -fx-text-fill: #F57F17; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold;");
        } else {
            lblPrioridadeCalculada.setText("BAIXO");
            lblPrioridadeCalculada.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold;");
        }
    }

    private void configurarSeletoresEntidade() {
        grpTipoAlvo.selectedToggleProperty().addListener((obs, antigo, novo) -> alternarTipoAlvo());

        cbEntidadeAlvo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Object obj) {
                if (obj instanceof Organizacao org) {
                    return org.getId() + " - " + org.getNome();
                } else if (obj instanceof Produtor prod) {
                    return prod.getId() + " - " + prod.getNome() + " (" + prod.getComunidade() + ")";
                }
                return "";
            }

            @Override
            public Object fromString(String string) {
                return null;
            }
        });

        cbEntidadeAlvo.getSelectionModel().selectedItemProperty().addListener((obs, antigo, selecionado) -> {
            if (!ignorarListener && selecionado != null) {
                carregarDadosEntidade(selecionado);
            }
        });
    }

    private void configurarTabela() {
        colProblema.setCellValueFactory(new PropertyValueFactory<>("problemaAcao"));
        colOrigem.setCellValueFactory(new PropertyValueFactory<>("origem"));
        colGravidade.setCellValueFactory(new PropertyValueFactory<>("gravidade"));
        colUrgencia.setCellValueFactory(new PropertyValueFactory<>("urgencia"));
        colTendencia.setCellValueFactory(new PropertyValueFactory<>("tendencia"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("scoreGut"));
        colPrioridade.setCellValueFactory(new PropertyValueFactory<>("nivelPrioridade"));
        colAcao.setCellValueFactory(new PropertyValueFactory<>("acaoProposta"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Destaque visual por nível de prioridade na coluna correspondente
        colPrioridade.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String prioridade, boolean empty) {
                super.updateItem(prioridade, empty);
                if (empty || prioridade == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(prioridade);
                    switch (prioridade.toUpperCase()) {
                        case "CRÍTICO" -> setStyle("-fx-background-color: #FFCDD2; -fx-text-fill: #B71C1C; -fx-alignment: CENTER; -fx-font-weight: bold;");
                        case "ALTO" -> setStyle("-fx-background-color: #FFE0B2; -fx-text-fill: #E65100; -fx-alignment: CENTER; -fx-font-weight: bold;");
                        case "MÉDIO" -> setStyle("-fx-background-color: #FFF9C4; -fx-text-fill: #F57F17; -fx-alignment: CENTER; -fx-font-weight: bold;");
                        default -> setStyle("-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32; -fx-alignment: CENTER; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tblGut.setItems(listaItensGut);
    }

    private void alternarTipoAlvo() {
        ignorarListener = true;
        try {
            cbEntidadeAlvo.getItems().clear();
            limparFormulario();
            listaItensGut.clear();
            atualizarContadores();

            if (rbTipoOrg.isSelected()) {
                lblEntidadeAlvo.setText("Organização:*");
                List<Organizacao> orgs = orgRepo.listarTodas();
                cbEntidadeAlvo.getItems().addAll(orgs);
            } else {
                lblEntidadeAlvo.setText("Produtor Rural:*");
                List<Produtor> prods = prodRepo.listarTodos();
                cbEntidadeAlvo.getItems().addAll(prods);
            }

            if (!cbEntidadeAlvo.getItems().isEmpty()) {
                cbEntidadeAlvo.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            exibirMensagemErro("Erro ao listar entidades: " + e.getMessage());
        } finally {
            ignorarListener = false;
        }

        if (cbEntidadeAlvo.getValue() != null) {
            carregarDadosEntidade(cbEntidadeAlvo.getValue());
        }
    }

    private void carregarDadosEntidade(Object entidade) {
        if (entidade == null) {
            listaItensGut.clear();
            atualizarContadores();
            return;
        }

        try {
            List<ItemGut> itens;
            if (entidade instanceof Organizacao org) {
                itens = gutRepo.listarPorOrganizacao(org.getId());
            } else {
                itens = gutRepo.listarPorProdutor(((Produtor) entidade).getId());
            }

            listaItensGut.setAll(itens);
            ordenarLista();
            atualizarContadores();

            Platform.runLater(() -> tblGut.refresh());

            lblStatusMensagem.setStyle("-fx-text-fill: #2e7d32;");
            lblStatusMensagem.setText("Dados da Matriz GUT carregados com sucesso.");
        } catch (Exception e) {
            exibirMensagemErro("Erro ao carregar dados GUT: " + e.getMessage());
        }
    }

    private void ordenarLista() {
        listaItensGut.sort(Comparator.comparingInt(ItemGut::getScoreGut)
                .thenComparingInt(ItemGut::getGravidade).reversed());
    }

    private void atualizarContadores() {
        long criticos = listaItensGut.stream().filter(i -> "CRÍTICO".equalsIgnoreCase(i.getNivelPrioridade())).count();
        long altos = listaItensGut.stream().filter(i -> "ALTO".equalsIgnoreCase(i.getNivelPrioridade())).count();
        long medios = listaItensGut.stream().filter(i -> "MÉDIO".equalsIgnoreCase(i.getNivelPrioridade())).count();
        long baixos = listaItensGut.stream().filter(i -> "BAIXO".equalsIgnoreCase(i.getNivelPrioridade())).count();

        lblCountCritico.setText(criticos + " Críticos (≥75)");
        lblCountAlto.setText(altos + " Altos (≥45)");
        lblCountMedio.setText(medios + " Médios (≥20)");
        lblCountBaixo.setText(baixos + " Baixos (<20)");
    }

    // ==========================================
    //            OPERAÇÕES: CRUD
    // ==========================================

    @FXML
    public void salvarItem() {
        Object entidade = cbEntidadeAlvo.getValue();
        if (entidade == null) {
            exibirMensagemErro("Selecione uma Cooperativa ou Produtor Rural antes de salvar!");
            return;
        }

        String problema = txtProblemaAcao.getText();
        if (problema == null || problema.trim().isEmpty()) {
            exibirMensagemErro("A descrição do problema ou iniciativa é obrigatória.");
            return;
        }

        try {
            boolean isNovo = (itemEmEdicao == null || itemEmEdicao.getId() == null);
            if (isNovo) {
                itemEmEdicao = new ItemGut();
                if (entidade instanceof Organizacao org) {
                    itemEmEdicao.setOrganizacaoId(org.getId());
                    itemEmEdicao.setProdutorId(null);
                } else {
                    itemEmEdicao.setProdutorId(((Produtor) entidade).getId());
                    itemEmEdicao.setOrganizacaoId(null);
                }
                itemEmEdicao.setOrigem("MANUAL");
            }

            itemEmEdicao.setProblemaAcao(problema.trim());
            itemEmEdicao.setGravidade((int) sldGravidade.getValue());
            itemEmEdicao.setUrgencia((int) sldUrgencia.getValue());
            itemEmEdicao.setTendencia((int) sldTendencia.getValue());
            itemEmEdicao.setAcaoProposta(txtAcaoProposta.getText() != null ? txtAcaoProposta.getText().trim() : "");
            itemEmEdicao.setStatus(cbStatus.getValue() != null ? cbStatus.getValue() : "PENDENTE");

            if (isNovo) {
                itemEmEdicao = gutRepo.salvar(itemEmEdicao);
                listaItensGut.add(itemEmEdicao);
                lblStatusMensagem.setStyle("-fx-text-fill: #2e7d32;");
                lblStatusMensagem.setText("Item registrado com prioridade " + itemEmEdicao.getNivelPrioridade() + ".");
            } else {
                gutRepo.atualizar(itemEmEdicao);
                lblStatusMensagem.setStyle("-fx-text-fill: #2e7d32;");
                lblStatusMensagem.setText("Item da Matriz GUT atualizado.");
            }

            ordenarLista();
            atualizarContadores();
            limparFormulario();
            Platform.runLater(() -> tblGut.refresh());
        } catch (Exception e) {
            exibirMensagemErro("Erro ao salvar item na Matriz GUT: " + e.getMessage());
        }
    }

    @FXML
    public void editarItemSelecionado() {
        ItemGut selecionado = tblGut.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            exibirMensagemErro("Selecione um item na tabela para editar.");
            return;
        }

        this.itemEmEdicao = selecionado;
        txtProblemaAcao.setText(selecionado.getProblemaAcao());
        sldGravidade.setValue(selecionado.getGravidade());
        sldUrgencia.setValue(selecionado.getUrgencia());
        sldTendencia.setValue(selecionado.getTendencia());
        txtAcaoProposta.setText(selecionado.getAcaoProposta());
        cbStatus.setValue(selecionado.getStatus());

        lblStatusMensagem.setStyle("-fx-text-fill: #1976D2;");
        lblStatusMensagem.setText("A editar item #" + selecionado.getId());
    }

    @FXML
    public void excluirItemSelecionado() {
        ItemGut selecionado = tblGut.getSelectionModel().getSelectedItem();
        if (selecionado == null || selecionado.getId() == null) {
            exibirMensagemErro("Selecione um item na tabela para excluir.");
            return;
        }

        try {
            int idParaRemover = selecionado.getId();
            gutRepo.excluir(idParaRemover);
            listaItensGut.removeIf(i -> i.getId() != null && i.getId().equals(idParaRemover));

            atualizarContadores();
            Platform.runLater(() -> tblGut.refresh());

            lblStatusMensagem.setStyle("-fx-text-fill: #2e7d32;");
            lblStatusMensagem.setText("Item removido com sucesso.");
        } catch (Exception e) {
            exibirMensagemErro("Erro ao excluir item: " + e.getMessage());
        }
    }

    // ==========================================
    //       IMPORTAÇÕES AUTOMATIZADAS
    // ==========================================

    @FXML
    public void importarFraquezasAmeacas() {
        Object entidade = cbEntidadeAlvo.getValue();
        if (entidade == null) {
            exibirMensagemErro("Selecione uma Cooperativa ou Produtor Rural antes de importar!");
            return;
        }

        try {
            Integer orgId = (entidade instanceof Organizacao org) ? org.getId() : null;
            Integer prodId = (entidade instanceof Produtor prod) ? prod.getId() : null;

            int total = gutRepo.importarFraquezasAmeacasSwot(orgId, prodId);
            lblStatusMensagem.setStyle("-fx-text-fill: #2e7d32;");
            lblStatusMensagem.setText("Importação concluída: " + total + " fraqueza(s)/ameaça(s) trazida(s) da FOFA.");
            carregarDadosEntidade(entidade);
        } catch (Exception e) {
            exibirMensagemErro("Erro ao importar dados da FOFA: " + e.getMessage());
        }
    }

    @FXML
    public void importarEstrategiasTows() {
        Object entidade = cbEntidadeAlvo.getValue();
        if (entidade == null) {
            exibirMensagemErro("Selecione uma Cooperativa ou Produtor Rural antes de importar!");
            return;
        }

        try {
            Integer orgId = (entidade instanceof Organizacao org) ? org.getId() : null;
            Integer prodId = (entidade instanceof Produtor prod) ? prod.getId() : null;

            int total = gutRepo.importarEstrategiasTows(orgId, prodId);
            lblStatusMensagem.setStyle("-fx-text-fill: #2e7d32;");
            lblStatusMensagem.setText("Importação concluída: " + total + " estratégia(s) trazida(s) da TOWS.");
            carregarDadosEntidade(entidade);
        } catch (Exception e) {
            exibirMensagemErro("Erro ao importar estratégias TOWS: " + e.getMessage());
        }
    }

    @FXML
    public void limparFormulario() {
        this.itemEmEdicao = null;
        txtProblemaAcao.clear();
        sldGravidade.setValue(3.0);
        sldUrgencia.setValue(3.0);
        sldTendencia.setValue(3.0);
        txtAcaoProposta.clear();
        cbStatus.getSelectionModel().selectFirst();
        recalcularPreviewScore();
    }

    private void exibirMensagemErro(String msg) {
        lblStatusMensagem.setStyle("-fx-text-fill: #d32f2f;");
        lblStatusMensagem.setText(msg);
    }
}