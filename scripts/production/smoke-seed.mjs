// Exclusivamente el stack temporal F18. Nunca se usa para generar cuentas production.
if (process.env.COMPOSE_PROJECT_NAME !== 'polleria-prodtest') throw Error('Solo prodtest');
const base='http://keycloak:8080/auth';
const tokenResponse=await fetch(base+'/realms/master/protocol/openid-connect/token',{
  method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},
  body:new URLSearchParams({grant_type:'password',client_id:'admin-cli',username:process.env.KEYCLOAK_ADMIN_USERNAME,password:process.env.KEYCLOAK_ADMIN_PASSWORD})
});
if(!tokenResponse.ok) throw Error('Bootstrap auth HTTP '+tokenResponse.status);
const {access_token}=await tokenResponse.json();
const headers={Authorization:'Bearer '+access_token,'Content-Type':'application/json'};
const users=await fetch(base+'/admin/realms/polleria/users?username=smoke-f18&exact=true',{headers}).then(r=>r.json());
if(!users.length){
 const r=await fetch(base+'/admin/realms/polleria/users',{method:'POST',headers,body:JSON.stringify({username:'smoke-f18',enabled:true,firstName:'Smoke',lastName:'F18',email:'smoke-f18@localhost',emailVerified:true,requiredActions:[],credentials:[{type:'password',temporary:false,value:process.env.SMOKE_PASSWORD}]})});
 if(r.status!==201) throw Error('Seed HTTP '+r.status);
}
const user=(await fetch(base+'/admin/realms/polleria/users?username=smoke-f18&exact=true',{headers}).then(r=>r.json()))[0];
const role=await fetch(base+'/admin/realms/polleria/roles/ADMINISTRADOR',{headers}).then(r=>r.json());
const r=await fetch(base+'/admin/realms/polleria/users/'+user.id+'/role-mappings/realm',{method:'POST',headers,body:JSON.stringify([role])});
if(!r.ok) throw Error('Role HTTP '+r.status);
console.log('Cuenta de smoke aislada preparada, sin revelar credenciales.');
