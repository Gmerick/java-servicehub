# ServiceHub 1.2.0 — demonstração com acesso protegido

Login em português, sessões com CSRF, logout, limite de tentativas e perfis ADMIN/VISITANTE. Visitantes consultam dados fictícios sem permissão de gravação no backend. Identidade visual e regras de negócio preservadas.

O primeiro início exige credenciais administrativas externas; não há senha padrão nem cadastro público. Consulte COMO-USAR.md para gerar o hash BCrypt em terminal privado. A execução usa Java17; gerar credenciais requer JDK17 e Python3. Java não acompanha o ZIP.

O perfil demo usa data-demo, separado do banco data existente. Antes de atualizar qualquer instalação, encerre o programa e faça backup consistente do banco e do JAR. Preserve os dados e suas credenciais externas. Não execute duas versões sobre o mesmo banco.

Caddy e perfil público HTTPS preparados para a mesma EC2, mas publicação depende de domínio, backup e corte autorizado. Este PR não publica nem modifica a instalação AWS. Consulte docs/PUBLIC-DEMO.md no repositório para implantação e recuperação.
