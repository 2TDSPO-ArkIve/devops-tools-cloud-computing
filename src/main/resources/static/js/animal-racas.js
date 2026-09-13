(() => {
    const especie = document.getElementById('especieId');
    const raca = document.getElementById('racaId');
    const salvar = document.getElementById('salvar-raca');
    const mensagem = document.getElementById('raca-mensagem');
    if (!especie || !raca || !salvar) return;

    const nome = document.getElementById('nova-raca-nome');
    const porte = document.getElementById('nova-raca-porte');
    let versao = 0;
    let criando = false;

    async function respostaJson(response) {
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            throw new Error(body.message || body.mensagem || 'Nao foi possivel carregar ou cadastrar a raca.');
        }
        return response.json();
    }

    async function carregar(selecionada = '') {
        const atual = ++versao;
        const especieId = especie.value;
        raca.replaceChildren(new Option('Sem raca informada', ''));
        raca.disabled = true;
        salvar.disabled = !especieId || criando;
        mensagem.textContent = '';
        if (!especieId) return;
        try {
            let pagina = 0;
            let dados;
            do {
                dados = await fetch(`/api/racas?especieId=${encodeURIComponent(especieId)}&size=100&page=${pagina}&sort=nome,asc`,
                    { headers: { Accept: 'application/json' } }).then(respostaJson);
                if (atual !== versao) return;
                for (const item of dados.content) {
                    if (String(item.especieId) === especieId) {
                        raca.add(new Option(item.nome, String(item.id)));
                    }
                }
                pagina++;
            } while (!dados.last);
            raca.value = Array.from(raca.options).some(option => option.value === String(selecionada)) ? String(selecionada) : '';
            raca.disabled = false;
        } catch (error) {
            if (atual === versao) mensagem.textContent = error.message;
        }
    }

    especie.addEventListener('change', () => carregar());
    salvar.addEventListener('click', async () => {
        const especieId = especie.value;
        if (!especieId || criando) return;
        if (!nome.value.trim()) {
            mensagem.textContent = 'Informe o nome da raca.';
            nome.focus();
            return;
        }
        criando = true;
        salvar.disabled = true;
        especie.disabled = true;
        try {
            const criada = await fetch('/api/racas', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify({ nome: nome.value.trim(), especieId: Number(especieId), porte: porte.value || null })
            }).then(respostaJson);
            await carregar(criada.id);
            nome.value = '';
            porte.value = '';
            document.getElementById('cadastro-raca').open = false;
        } catch (error) {
            mensagem.textContent = error.message;
        } finally {
            criando = false;
            especie.disabled = false;
            salvar.disabled = !especie.value;
        }
    });
    especie.form.addEventListener('submit', event => {
        if (criando) event.preventDefault();
    });
    carregar(raca.value);
})();
