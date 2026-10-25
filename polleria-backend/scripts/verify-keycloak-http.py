#!/usr/bin/env python3
"""Real local HTTP role matrix. JWTs/passwords stay in memory; one cancelled test order remains."""
import argparse
import base64
import importlib
import json
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen

seed = importlib.import_module('keycloak-dev-users')

def api(path, token=None, method='GET', data=None, headers=None):
    request_headers = dict(headers or {})
    if token: request_headers['Authorization'] = 'Bearer ' + token
    if data is not None:
        request_headers['Content-Type'] = 'application/json'
        data = json.dumps(data).encode()
    try:
        response = urlopen(Request('http://localhost:8080' + path, data=data, headers=request_headers, method=method), timeout=30)
    except HTTPError as error:
        response = error
    with response:
        assert not any('JSESSIONID=' in value for value in response.headers.get_all('Set-Cookie', []))
        raw = response.read()
        try: body = json.loads(raw) if raw else None
        except json.JSONDecodeError: body = None
        return response.status, dict(response.headers), body

def expect(result, status, label):
    if result[0] != status:
        raise AssertionError(f'{label}: expected {status}, got {result[0]}')
    return result[2]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cliente-id', type=int, required=True)
    parser.add_argument('--producto-id', type=int, required=True)
    args = parser.parse_args()
    env = seed.environment()
    tokens = {}
    report = {}
    for role in seed.ROLES:
        token = seed.request('/realms/polleria/protocol/openid-connect/token', {
            'grant_type': 'password', 'client_id': 'polleria-frontend', 'username': role.lower(),
            'password': env['KEYCLOAK_' + role + '_PASSWORD'],
        }, form=True)['access_token']
        claims = json.loads(base64.urlsafe_b64decode(token.split('.')[1] + '==='))
        assert claims['iss'] == 'http://localhost:38080/realms/polleria'
        uuid.UUID(claims['sub'])
        assert role in claims['realm_access']['roles']
        tokens[role] = token
        report[role] = {'issuerCorrect': True, 'subStringUUID': True, 'realmRoleCorrect': True}

    expect(api('/api/v1/categorias'), 200, 'Public categories')
    catalog = expect(api('/api/v1/productos'), 200, 'Public products')
    current = next(product for product in catalog if product['id'] == args.producto_id)
    expect(api('/api/v1/productos/' + str(args.producto_id)), 200, 'Public product by id')
    expect(api('/api/v1/categorias/' + str(current['categoria']['id'])), 200, 'Public category by id')
    for path in ['/api/v1/pedidos', '/api/v1/clientes', '/api/v1/pedidos/reportes/resumen']:
        expect(api(path), 401, 'Anonymous protected endpoint')
    expect(api('/api/v1/categorias', method='POST', data={'nombre':'Denied','activo':True}), 401, 'Anonymous catalog write')
    expect(api('/api/v1/pedidos', 'invalid-token'), 401, 'Malformed token')
    pieces = tokens['ADMINISTRADOR'].split('.')
    pieces[2] = ('A' if pieces[2][0] != 'A' else 'B') + pieces[2][1:]
    expect(api('/api/v1/pedidos', '.'.join(pieces)), 401, 'Tampered signature')
    report['anonymous'] = {'catalogRead':200, 'catalogWrite':401, 'orders':401, 'reports':401, 'malformedToken':401, 'tamperedSignature':401}

    admin = tokens['ADMINISTRADOR']
    category_id = None
    try:
        created = expect(api('/api/v1/categorias', admin, 'POST', {'nombre':'Security HTTP ' + str(uuid.uuid4()),'activo':True}), 201, 'Administrator category create')
        category_id = created['id']
        expect(api(f'/api/v1/categorias/{category_id}', admin, 'PUT', {'nombre':created['nombre'],'activo':False}), 200, 'Administrator category update')
    finally:
        if category_id is not None: expect(api(f'/api/v1/categorias/{category_id}', admin, 'DELETE'), 204, 'Administrator category cleanup')
    expect(api('/api/v1/pedidos/reportes/resumen', admin), 200, 'Administrator reports')
    report['ADMINISTRADOR'].update({'catalogCreate':201,'catalogUpdate':200,'catalogCleanup':204,'reports':200})

    for role in ['CAJERO','CLIENTE','REPARTIDOR','ABASTECIMIENTO']:
        token = tokens[role]
        expect(api('/api/v1/productos', token), 200, role + ' catalog read')
        denied = api('/api/v1/categorias', token, 'POST', {'nombre':'Denied','activo':True}, {'X-Trace-ID':'security-real-' + role.lower()})
        expect(denied, 403, role + ' catalog write')
        assert denied[1].get('X-Trace-ID') == 'security-real-' + role.lower()
        assert denied[2]['traceId'] == denied[1]['X-Trace-ID']
        expect(api('/api/v1/pedidos/reportes/resumen', token), 403, role + ' report denied')
        report[role].update({'catalogRead':200,'catalogWrite':403,'reports':403,'traceId':True})
        if role != 'CAJERO':
            for path in ['/api/v1/pedidos', '/api/v1/pedidos/175', '/api/v1/clientes']:
                expect(api(path, token), 403, role + ' protected resource denied')
            report[role].update({'globalOrders':403,'arbitraryOrder':403,'customers':403})

    cashier = tokens['CAJERO']
    expect(api('/api/v1/clientes', cashier), 200, 'Cashier customer read')
    order_id = None
    try:
        order = expect(api('/api/v1/pedidos', cashier, 'POST', {
            'clienteId':args.cliente_id,'tipoPedido':'RECOJO','metodoPago':'EFECTIVO',
            'direccionEntrega':None,'referenciaEntrega':None,
            'detalles':[{'productoId':args.producto_id,'cantidad':1}],
        }), 201, 'Cashier order create')
        order_id = order['id']
        assert order['estado'] == 'PENDIENTE' and order['estadoPago'] == 'PENDIENTE'
        assert order['total'] == current['precio']
        report['CAJERO'].update({'createOrder':201,'orderId':order_id,'total':order['total']})
    finally:
        if order_id is not None:
            expect(api(f'/api/v1/pedidos/{order_id}/estado', cashier, 'PATCH', {'estado':'CANCELADO'}), 200, 'Cashier cancel test order')
    report['CAJERO']['testOrderFinalState'] = 'CANCELADO'

    health = expect(api('/actuator/health'), 200, 'Public health')
    assert 'components' not in health
    expect(api('/actuator/info'), 401, 'Anonymous actuator info')
    expect(api('/actuator/info', cashier), 403, 'Cashier actuator info')
    expect(api('/actuator/info', admin), 200, 'Administrator actuator info')
    docs = expect(api('/v3/api-docs'), 200, 'OpenAPI')
    assert docs['components']['securitySchemes']['bearerAuth']['scheme'] == 'bearer'
    assert docs['paths']['/api/v1/productos']['get']['security'] == []
    preflight = api('/api/v1/pedidos', method='OPTIONS', headers={
        'Origin':'http://localhost:4200','Access-Control-Request-Method':'POST',
        'Access-Control-Request-Headers':'authorization,content-type,x-trace-id',
    })
    expect(preflight, 200, 'CORS preflight')
    assert preflight[1]['Access-Control-Allow-Origin'] == 'http://localhost:4200'
    trace = api('/api/v1/pedidos', headers={'Origin':'http://localhost:4200','X-Trace-ID':'security-real-401'})
    expect(trace, 401, 'CORS unauthorized')
    assert trace[1]['Access-Control-Allow-Origin'] == 'http://localhost:4200'
    assert trace[2]['traceId'] == 'security-real-401'
    report['infrastructure'] = {'health':200,'healthDetailsHidden':True,'swaggerBearer':True,'corsPreflight':200,'corsOn401':True,'traceIdOn401':True,'stateless': 'no application session cookies observed'}
    print(json.dumps(report, indent=2))

if __name__ == '__main__':
    main()
