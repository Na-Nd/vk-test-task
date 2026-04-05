box.ctl.wait_rw()   -- Ожидание перехода в RW

if not box.space.KV then
    local kv = box.schema.space.create('KV', {
        format = {
            { name = 'key',   type = 'string'                      },
            { name = 'value', type = 'varbinary', is_nullable = true },
        },
        if_not_exists = true,
    })

    kv:create_index('primary', {
        type          = 'TREE',
        parts         = { { field = 'key', type = 'string' } },
        if_not_exists = true,
    })

    print('[init] Space KV created successfully')
else
    print('[init] Space KV already exists')
end