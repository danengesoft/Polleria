#!/usr/bin/env python3
"""Verificación local F17: Code+PKCE, roles, validación, transacciones; datos F17 conservados."""
from pathlib import Path
import base64,hashlib,secrets,sys,json,http.cookiejar,importlib
from html.parser import HTMLParser
from urllib.parse import urlencode,urlparse,parse_qs
from urllib.request import build_opener,HTTPRedirectHandler,HTTPCookieProcessor,Request,urlopen
from urllib.error import HTTPError
sys.path.insert(0,'/home/systemseng/Polleria/polleria-backend/scripts')
s=importlib.import_module('keycloak-dev-users');env=s.environment()
issuer='http://localhost:38080/realms/polleria';client='polleria-frontend';callback='http://localhost:4200/auth/callback'
class StopRedirect(HTTPRedirectHandler):
 def redirect_request(self,*args,**kwargs):return None
class Form(HTMLParser):
 action=None
 def handle_starttag(self,tag,attrs):
  a=dict(attrs)
  if tag=='form' and a.get('id')=='kc-form-login':self.action=a['action']
def login(role):
 verifier=secrets.token_urlsafe(32);state=secrets.token_urlsafe(16)
 challenge=base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).decode().rstrip('=')
 jar=http.cookiejar.CookieJar();opener=build_opener(HTTPCookieProcessor(jar),StopRedirect())
 params={'client_id':client,'redirect_uri':callback,'response_type':'code','scope':'openid profile email','state':state,'code_challenge':challenge,'code_challenge_method':'S256'}
 page=opener.open(issuer+'/protocol/openid-connect/auth?'+urlencode(params)).read().decode();form=Form();form.feed(page);assert form.action
 assert urlparse(form.action).netloc == 'localhost:38080'
 cookie_header='; '.join(c.name+'='+c.value for c in jar)
 try:opener.open(Request(form.action,data=urlencode({'username':role.lower(),'password':env['KEYCLOAK_'+role+'_PASSWORD'],'credentialId':''}).encode(),headers={'Content-Type':'application/x-www-form-urlencoded','Cookie':cookie_header}))
 except HTTPError as response:
  if response.code != 302:
   import re
   body=response.read().decode(); print("OAuth diagnostic:", response.code, re.findall(r'<(?:span|p)[^>]*(?:kc-feedback-text|instruction)[^>]*>(.*?)</(?:span|p)>',body), file=sys.stderr)
  assert response.code==302
  query=parse_qs(urlparse(response.headers['Location']).query);assert query['state'][0]==state
  code=query['code'][0]
 with urlopen(Request(issuer+'/protocol/openid-connect/token',data=urlencode({'grant_type':'authorization_code','client_id':client,'redirect_uri':callback,'code':code,'code_verifier':verifier}).encode(),headers={'Content-Type':'application/x-www-form-urlencoded'})) as result:
  return json.load(result)
def api(path,token,method='GET',data=None):
 headers={'Authorization':'Bearer '+token}
 if data is not None:headers['Content-Type']='application/json';data=json.dumps(data).encode()
 try:r=urlopen(Request('http://localhost:8080'+path,headers=headers,method=method,data=data))
 except HTTPError as error:r=error
 with r:return r.status

from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
import subprocess,uuid

result={'requests':[],'created':{},'checks':{},'completado':False}
tokens={}
def request(path,role=None,method='GET',body=None,extra=None,raw=None):
    headers={'X-Trace-ID':'fase17-http'}
    if role:headers['Authorization']='Bearer '+tokens[role]['access_token']
    if body is not None:headers['Content-Type']='application/json';raw=json.dumps(body).encode()
    headers.update(extra or {})
    try:r=urlopen(Request('http://localhost:8080'+path,data=raw,headers=headers,method=method),timeout=30)
    except HTTPError as e:r=e
    with r:
        data=r.read();status=r.status;response_headers=dict(r.headers)
        try:data=json.loads(data) if data else None
        except (json.JSONDecodeError,UnicodeDecodeError):data=None
    assert response_headers.get('X-Trace-ID')=='fase17-http',(path,'trace ausente')
    result['requests'].append({'method':method,'path':path,'role':role or 'ANONIMO','http':status})
    return status,data,response_headers

def expect(path,role=None,status=200,method='GET',body=None,**kwargs):
    code,data,headers=request(path,role,method,body,**kwargs)
    assert code==status,(path,role,code,status,data.get('message') if isinstance(data,dict) else None)
    if code>=400 and isinstance(data,dict):assert not any(x in json.dumps(data) for x in ['ORA-','java.lang.','SELECT *','password='])
    return data

def db_snapshot():
    sql="""SET HEADING OFF FEEDBACK OFF PAGESIZE 0 LINESIZE 4000
ALTER SESSION SET CONTAINER=FREEPDB1;
SELECT JSON_OBJECT('pedidos' VALUE (SELECT COUNT(*) FROM POLLERIA.PEDIDOS),'detalles' VALUE (SELECT COUNT(*) FROM POLLERIA.DETALLE_PEDIDOS),'compras' VALUE (SELECT COUNT(*) FROM POLLERIA.COMPRAS),'detallesCompra' VALUE (SELECT COUNT(*) FROM POLLERIA.DETALLE_COMPRAS),'movimientos' VALUE (SELECT COUNT(*) FROM POLLERIA.MOVIMIENTOS_INVENTARIO),'stock84' VALUE (SELECT STOCK_ACTUAL FROM POLLERIA.INSUMOS WHERE ID=84)) FROM DUAL;
EXIT
"""
    out=subprocess.check_output(['docker','exec','-i','polleria-oracle','sqlplus','-s','/','as','sysdba'],input=sql,text=True)
    return next(json.loads(l) for l in out.splitlines() if l.startswith('{'))

def profile_payload(p):return {k:p.get(k) for k in ['nombres','apellidos','telefono','correo','direccionPrincipal','referencia']}
def product_payload(p):return {**{k:p.get(k) for k in ['nombre','descripcion','precio','disponible','imagenUrl']},'categoriaId':p['categoria']['id']}
def insumo_payload(p):return {k:p.get(k) for k in ['nombre','descripcion','unidadMedida','activo']}
def pedido_payload(metodo='EFECTIVO',tipo='RECOJO'):
    return {'tipoPedido':tipo,'metodoPago':metodo,'direccionEntrega':'Dirección histórica controlada F17' if tipo=='DELIVERY' else None,'referenciaEntrega':'Prueba integral F17' if tipo=='DELIVERY' else None,'detalles':[{'productoId':10,'cantidad':1}]}

def oidc_logout(t):
    with urlopen(Request(issuer+'/protocol/openid-connect/logout',data=urlencode({'client_id':client,'refresh_token':t['refresh_token']}).encode(),headers={'Content-Type':'application/x-www-form-urlencoded'}),timeout=30) as r:assert r.status==204

try:
    for role in ['ADMINISTRADOR','CAJERO','REPARTIDOR','ABASTECIMIENTO','CLIENTE','CLIENTE_B','REPARTIDOR_B']:tokens[role]=login(role)
    result['checks']['codePkceStateCallback']=True
    expect('/api/v1/productos');expect('/api/v1/categorias');expect('/api/v1/pedidos',status=401)
    for invalid in ['malformed','.'.join(tokens['ADMINISTRADOR']['access_token'].split('.')[:2])+'.invalid-signature']:
        expect('/api/v1/pedidos',status=401,extra={'Authorization':'Bearer '+invalid})
    matrix={'/api/v1/clientes':{'ADMINISTRADOR','CAJERO'},'/api/v1/pedidos':{'ADMINISTRADOR','CAJERO'},'/api/v1/delivery/entregas':{'ADMINISTRADOR','CAJERO'},'/api/v1/abastecimiento/insumos':{'ADMINISTRADOR','ABASTECIMIENTO'},'/api/v1/pedidos/reportes/resumen':{'ADMINISTRADOR'}}
    for path,allowed in matrix.items():
        for role in ['ADMINISTRADOR','CAJERO','REPARTIDOR','ABASTECIMIENTO','CLIENTE']:expect(path,role,200 if role in allowed else 403)
    for role in ['CAJERO','REPARTIDOR','ABASTECIMIENTO','CLIENTE']:expect('/api/v1/categorias',role,403,'POST',{'nombre':'No autorizado','activo':True})
    expect('/api/v1/clientes/676','CAJERO',403,'DELETE')
    result['checks']['matrizRoles']=True
    # A temporary, unreferenced catalogue fixture exercises authorized CRUD; only its own IDs are removed.
    cat=prod=None
    try:
        cat=expect('/api/v1/categorias','ADMINISTRADOR',201,'POST',{'nombre':'Prueba integral F17 '+str(uuid.uuid4()),'activo':True})
        prod=expect('/api/v1/productos','ADMINISTRADOR',201,'POST',{'nombre':'Temporal F17','precio':5,'disponible':True,'categoriaId':cat['id']})
        expect('/api/v1/productos/'+str(prod['id']),'ADMINISTRADOR',200,'PUT',{**product_payload(prod),'precio':6})
        expect('/api/v1/categorias/'+str(cat['id']),'ADMINISTRADOR',409,'DELETE')
    finally:
        if prod:expect('/api/v1/productos/'+str(prod['id']),'ADMINISTRADOR',204,'DELETE')
        if cat:expect('/api/v1/categorias/'+str(cat['id']),'ADMINISTRADOR',204,'DELETE')
    c=expect('/api/v1/clientes','CAJERO',201,'POST',{'nombres':'Prueba','apellidos':'Integral F17','telefono':'999000017'})
    expect('/api/v1/clientes/'+str(c['id']),'CAJERO',200,'PUT',{'nombres':'Prueba','apellidos':'Integral F17 actualizado','telefono':'999000017','activo':False})
    expect('/api/v1/clientes/'+str(c['id']),'CAJERO',403,'DELETE');result['created']['clienteInterno']=c['id']
    # Wide input validation, always without accepting a write.
    for n in [0,-1,1.5]:
        p=pedido_payload();p['detalles'][0]['cantidad']=n;expect('/api/v1/pedidos/mios','CLIENTE',400,'POST',p)
    p=pedido_payload(tipo='DELIVERY');p['direccionEntrega']='   ';expect('/api/v1/pedidos/mios','CLIENTE',400,'POST',p)
    expect('/api/v1/clientes','CAJERO',400,'POST',{'nombres':' ','apellidos':'F17','telefono':'999000017','correo':'invalido'})
    expect('/api/v1/abastecimiento/proveedores','ABASTECIMIENTO',400,'POST',{'razonSocial':'F17','ruc':'123','activo':True})
    expect('/api/v1/pedidos/reportes/resumen?fechaDesde=2026-10-25&fechaHasta=2026-10-24','ADMINISTRADOR',400)
    expect('/api/v1/productos/9223372036854770000',status=404)
    expect('/api/v1/categorias','ADMINISTRADOR',409,'POST',{'nombre':'  pollos  ','activo':True})
    expect('/api/v1/categorias','ADMINISTRADOR',415,'POST',extra={'Content-Type':'text/plain'},raw=b'{"nombre":"F17","activo":true}')
    expect('/api/v1/productos',status=406,extra={'Accept':'application/xml'})
    result['checks']['validacionSin500']=True
    # Transactions: a valid first detail followed by a nonexistent resource.
    before=db_snapshot();p=pedido_payload();p['detalles'].append({'productoId':9223372036854770000,'cantidad':1})
    expect('/api/v1/pedidos/mios','CLIENTE',404,'POST',p);assert db_snapshot()==before
    result['checks']['rollbackPedido']=True
    expect('/api/v1/abastecimiento/compras','ABASTECIMIENTO',404,'POST',{'proveedorId':44,'detalles':[{'insumoId':84,'cantidad':1,'costoUnitario':2},{'insumoId':9223372036854770000,'cantidad':1,'costoUnitario':2}]});assert db_snapshot()==before
    result['checks']['rollbackCompra']=True
    # Authority comes from JWT and server calculation, never request fields.
    p=pedido_payload();p.update({'clienteId':677,'total':.01,'estado':'ENTREGADO','estadoPago':'PAGADO'});p['detalles'][0]['precioUnitario']=.01
    own=expect('/api/v1/pedidos/mios','CLIENTE',201,'POST',p);pid=own['id'];result['created']['pedidoRecojo']=pid
    assert own['cliente']['id']==676 and own['total']==20 and own['estado']=='PENDIENTE' and own['estadoPago']=='PENDIENTE'
    assert not any(e['pedidoId']==pid for e in expect('/api/v1/delivery/entregas','ADMINISTRADOR'))
    expect('/api/v1/pedidos/mios/'+str(pid),'CLIENTE_B',404)
    expect('/api/v1/pedidos/mios/'+str(pid)+'/cancelar','CLIENTE_B',404,'PATCH',{})
    assert expect('/api/v1/pedidos/mios/'+str(pid),'CLIENTE')['estado']=='PENDIENTE'
    expect('/api/v1/pedidos/'+str(pid)+'/estado','CAJERO',409,'PATCH',{'estado':'ENTREGADO'})
    expect('/api/v1/pedidos/1737/estado','CAJERO',409,'PATCH',{'estado':'PENDIENTE'})
    expect('/api/v1/pedidos/176/estado','CAJERO',409,'PATCH',{'estado':'CONFIRMADO'})
    expect('/api/v1/pedidos/mios/'+str(pid)+'/cancelar','CLIENTE',200,'PATCH',{})
    result['checks'].update({'idorCliente':True,'transicionesPedido':True,'checkoutRecojoSinEntrega':True,'massAssignmentPedido':True})
    a=expect('/api/v1/clientes/me','CLIENTE');b=expect('/api/v1/clientes/me','CLIENTE_B');assert a['id']!=b['id']
    mass={**profile_payload(a),'keycloakSub':'intruso','rol':'ADMINISTRADOR','activo':False}
    changed=expect('/api/v1/clientes/me','CLIENTE',200,'PUT',mass);assert changed['id']==a['id'] and changed['activo']==a['activo'] and 'keycloakSub' not in changed
    assert expect('/api/v1/clientes/me','CLIENTE_B')['id']==b['id']
    expect('/api/v1/categorias','CLIENTE',403,'POST',{'nombre':'No autorizado','activo':True})
    ra=expect('/api/v1/delivery/repartidores/me','REPARTIDOR');rb=expect('/api/v1/delivery/repartidores/me','REPARTIDOR_B');assert ra['id']!=rb['id']
    expect('/api/v1/delivery/repartidores/me','REPARTIDOR',409,'POST',{'keycloakSub':'intruso','rol':'ADMINISTRADOR'})
    assert expect('/api/v1/delivery/repartidores/me','REPARTIDOR')==ra
    for op,method in [('', 'GET'),('/iniciar','PATCH'),('/entregar','PATCH'),('/no-entregado','PATCH')]:expect('/api/v1/delivery/entregas/mias/21'+op,'REPARTIDOR_B',404,method,{'observacion':'No autorizado'} if method=='PATCH' else None)
    minimal=expect('/api/v1/delivery/entregas/mias/21','REPARTIDOR');assert 'keycloakSub' not in json.dumps(minimal) and 'correo' not in json.dumps(minimal)
    result['checks'].update({'idorRepartidor':True,'massAssignmentPerfil':True,'massAssignmentRepartidor':True,'privacidad':True})
    # Restore any real data temporarily changed, even if an assertion fails.
    original=expect('/api/v1/productos/10');historico=expect('/api/v1/pedidos/mios/1737','CLIENTE')
    try:
        expect('/api/v1/productos/10','ADMINISTRADOR',200,'PUT',{**product_payload(original),'precio':25})
        assert expect('/api/v1/pedidos/mios/1737','CLIENTE')==historico
    finally:expect('/api/v1/productos/10','ADMINISTRADOR',200,'PUT',product_payload(original))
    try:
        expect('/api/v1/clientes/me','CLIENTE',200,'PUT',{**profile_payload(a),'direccionPrincipal':'Dirección temporal F17'})
        assert expect('/api/v1/pedidos/mios/1737','CLIENTE')['direccionEntrega']==historico['direccionEntrega']
        assert expect('/api/v1/delivery/entregas/mias/21','REPARTIDOR')['pedido']['direccionEntrega']==historico['direccionEntrega']
    finally:expect('/api/v1/clientes/me','CLIENTE',200,'PUT',profile_payload(a))
    insumo=expect('/api/v1/abastecimiento/insumos/84','ABASTECIMIENTO');compra=expect('/api/v1/abastecimiento/compras/7','ABASTECIMIENTO')
    try:
        expect('/api/v1/abastecimiento/insumos/84','ABASTECIMIENTO',200,'PUT',{**insumo_payload(insumo),'nombre':'Nombre temporal F17'})
        assert expect('/api/v1/abastecimiento/compras/7','ABASTECIMIENTO')==compra
    finally:expect('/api/v1/abastecimiento/insumos/84','ABASTECIMIENTO',200,'PUT',insumo_payload(insumo))
    result['checks'].update({'precioHistorico':True,'direccionHistorica':True,'compraHistorica':True,'restaurados':True})
    # Real delivered digital order with failed first attempt and reassignment.
    electronic=expect('/api/v1/pedidos/mios','CLIENTE',201,'POST',pedido_payload('YAPE','DELIVERY'));ep=electronic['id'];result['created']['pedidoYape']=ep
    for estado in ['CONFIRMADO','EN_PREPARACION','LISTO']:expect('/api/v1/pedidos/'+str(ep)+'/estado','CAJERO',200,'PATCH',{'estado':estado})
    e=expect('/api/v1/delivery/entregas','CAJERO',201,'POST',{'pedidoId':ep,'repartidorId':ra['id']});eid=e['id'];result['created']['entregaYape']=eid
    expect('/api/v1/delivery/entregas/mias/'+str(eid)+'/entregar','REPARTIDOR',409,'PATCH',{})
    e=expect('/api/v1/delivery/entregas/mias/'+str(eid)+'/iniciar','REPARTIDOR',200,'PATCH',{});assert e['pedido']['estadoPedido']=='DESPACHADO'
    e=expect('/api/v1/delivery/entregas/mias/'+str(eid)+'/no-entregado','REPARTIDOR',200,'PATCH',{'observacion':'Prueba integral F17: cliente ausente'});assert e['estado']=='NO_ENTREGADO' and e['pedido']['estadoPedido']=='DESPACHADO'
    e=expect('/api/v1/delivery/entregas/'+str(eid)+'/reasignar','CAJERO',200,'PATCH',{'repartidorId':rb['id']});assert e['estado']=='ASIGNADO' and e['pedido']['estadoPedido']=='DESPACHADO'
    e=expect('/api/v1/delivery/entregas/mias/'+str(eid)+'/iniciar','REPARTIDOR_B',200,'PATCH',{})
    e=expect('/api/v1/delivery/entregas/mias/'+str(eid)+'/entregar','REPARTIDOR_B',200,'PATCH',{});assert e['estado']=='ENTREGADO' and e['pedido']['estadoPedido']=='ENTREGADO' and e['pedido']['estadoPago']=='PENDIENTE'
    expect('/api/v1/pedidos/'+str(ep)+'/confirmar-pago','REPARTIDOR_B',403,'PATCH',{})
    paid=expect('/api/v1/pedidos/'+str(ep)+'/confirmar-pago','CAJERO',200,'PATCH',{});assert paid['estadoPago']=='PAGADO'
    expect('/api/v1/pedidos/'+str(ep)+'/confirmar-pago','CAJERO',409,'PATCH',{})
    result['checks'].update({'noEntregadoReasignacion':True,'pagoDigitalExplicito':True,'sincronizacionDelivery':True})
    # One small competing-stock scenario. Its movements remain as clearly labelled history.
    stock=expect('/api/v1/abastecimiento/insumos','ABASTECIMIENTO',201,'POST',{'nombre':'Concurrencia F17 '+str(uuid.uuid4())[:8],'unidadMedida':'KG','activo':True});sid=stock['id'];result['created']['insumoConcurrencia']=sid
    expect('/api/v1/abastecimiento/movimientos/ajustes','ABASTECIMIENTO',201,'POST',{'insumoId':sid,'nuevoStock':10,'motivo':'Conteo inicial prueba concurrencia F17'})
    def salida(_):return request('/api/v1/abastecimiento/movimientos/salidas','ABASTECIMIENTO','POST',{'insumoId':sid,'cantidad':8,'motivo':'Competencia controlada F17'})[0]
    with ThreadPoolExecutor(max_workers=2) as pool:statuses=list(pool.map(salida,range(2)))
    assert sorted(statuses)==[201,409],statuses
    assert expect('/api/v1/abastecimiento/insumos/'+str(sid),'ABASTECIMIENTO')['stockActual']==2
    result['checks']['concurrencia']={'statuses':statuses,'stockFinal':2}
    # CORS, tracing, exposure and current documentation.
    _,_,h=request('/api/v1/productos',extra={'Origin':'http://localhost:4200'});assert h.get('Access-Control-Allow-Origin')=='http://localhost:4200'
    code,_,h=request('/api/v1/productos',extra={'Origin':'https://origen-no-autorizado.invalid'});assert code==403 and 'Access-Control-Allow-Origin' not in h
    health=expect('/actuator/health');assert health['status']=='UP'
    for p in ['env','beans','metrics','configprops']:expect('/actuator/'+p,'ADMINISTRADOR',404)
    spec=expect('/v3/api-docs');assert spec['info']['title']=='POLLERIA API' and 'bearerAuth' in spec['components']['securitySchemes']
    operation_ids=[v['operationId'] for p in spec['paths'].values() for v in p.values() if isinstance(v,dict) and 'operationId' in v];assert len(operation_ids)==len(set(operation_ids))
    result['checks'].update({'cors':True,'traceId':True,'actuator':'UP','openapiSinDuplicados':True,'actuatorSinEndpointsSensibles':True})
    # Real token refresh and logout. Secrets never leave memory or get logged.
    old=tokens['CLIENTE_B']
    with urlopen(Request(issuer+'/protocol/openid-connect/token',data=urlencode({'grant_type':'refresh_token','client_id':client,'refresh_token':old['refresh_token']}).encode(),headers={'Content-Type':'application/x-www-form-urlencoded'})) as r:tokens['CLIENTE_B']=json.load(r)
    expect('/api/v1/clientes/me','CLIENTE_B');oidc_logout(tokens['CLIENTE_B'])
    try:urlopen(Request(issuer+'/protocol/openid-connect/token',data=urlencode({'grant_type':'refresh_token','client_id':client,'refresh_token':tokens['CLIENTE_B']['refresh_token']}).encode(),headers={'Content-Type':'application/x-www-form-urlencoded'}))
    except HTTPError as err:assert err.code==400
    else:raise AssertionError('Refresh después de logout no debe funcionar')
    result['checks']['refreshLogoutReal']=True
    result['checks']['sin500']=not any(x['http']>=500 for x in result['requests'])
    result['completado']=True
    Path('verificacion-http-fase-17.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n')
    print(json.dumps({'checks':result['checks'],'created':result['created'],'requests':len(result['requests'])},ensure_ascii=False))
finally:
    Path('verificacion-http-fase-17.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n')
    for t in tokens.values():oidc_logout(t)
