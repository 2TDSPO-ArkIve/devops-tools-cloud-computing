# Cronograma de Desenvolvimento — ArkIve

O desenvolvimento foi organizado em etapas incrementais até a preparação da aplicação Spring Boot para a entrega da Sprint 3 de Java Advanced.

| Etapa | Principais atividades | Status |
| --- | --- | --- |
| 1. Modelagem inicial e banco Oracle | Definição das entidades, relacionamentos, constraints e estrutura `TB_ARKIVE_*`. | Concluído |
| 2. Estrutura Spring Boot e API REST | Organização do projeto em camadas e criação dos controllers REST. | Concluído |
| 3. Persistência com JPA | Implementação das entities, repositories e services com Spring Data JPA e Hibernate. | Concluído |
| 4. Versionamento com Flyway | Criação e evolução do schema Oracle pelas migrations `V1` a `V6`. | Concluído |
| 5. Autenticação e Spring Security | Usuários no banco, BCrypt, form login, HTTP Basic e troca obrigatória da senha inicial. | Concluído |
| 6. Perfis e autorização | Regras de rota e de escopo para `SYSADMIN`, `ADMIN_CLINICA`, `VETERINARIO` e `RESPONSAVEL`. | Concluído |
| 7. Fluxo clínico assistido | Consulta, narrativa, transições de estado, apoio clínico externo e conclusão veterinária. | Concluído |
| 8. Prescrição e adesão | Prescrição após consulta finalizada e registro imutável de adesão pelo responsável vinculado. | Concluído |
| 9. Frontend Thymeleaf | Login, dashboards, cadastros administrativos, formulários e visualização clínica para os perfis web atendidos. | Concluído |
| 10. Transcrição e integrações | Integração com o motor clínico ArkIve e transcrição opcional com Azure Speech e FFmpeg. | Concluído |
| 11. Testes e correções | Testes automatizados, validação dos fluxos e reforço das regras de autorização REST. | Concluído |
| 12. Documentação e entrega | Atualização do README, arquitetura, coleção Postman e artefatos de apoio à demonstração. | Concluído |
