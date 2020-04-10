import random
import copy

#preco=[0.33 for i in range(24)]  #R$/kWh
#preco[18:24] = [0.56 for i in range(18, 24)]
#preco=[0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.44, 0.77, 1.23, 1.23, 1.23, 0.77, 0.44, 0.44]
preco=[0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.52, 0.88, 0.88, 1.40, 1.40, 1.40, 0.88, 0.52, 0.52]

#perfil pessoal
#pot=[0.09, 0.5, 0.03, 0.3, 0.0035, 1.4, 1, 5.5, 2, 0.6] #kW/h
#perfil=[0, 24, 6, 12, 1, 0, 0, 2, 3, 1] #horas ligadas por dia, definidas pelo usuario
#perfil=[8, 24, 6, 18, 1, 3, 1, 1, 1, 1] #horas ligadas por dia, definidas pelo usuario
#perfil = [18, 18, 18, 18, 18, 18, 18, 18, 18, 18] #perfil de teste

#agente01/02 classe baixa
#pot=[0.08, 0.08, 0.015, 0.01, 0.0035, 1.4, 1, 5.5, 2, 0.6] #kWh 
#perfil = [16, 24, 6, 1, 0, 0, 0, 0, 0, 0]
#resultado: 2.01 R$/dia, 60.41 R$/mes, 321 geracoes
#resultado: 2.013600 R$/dia, 60.408000 R$/mes, 817 geracoes

#agente03/04 classe media baixa
#pot=[0.180, 0.120, 0.02, 0.1, 0.01, 1.4, 1, 5.5, 2, 0.6] #kWh
#perfil=[16, 24, 6, 5, 1, 0, 0, 0, 0, 0]
#resultado: 3.769200 R$/dia, 113.076000 R$/mes, 422 geracoes

#agente05/06 classe media alta
#pot=[0.220, 0.200, 0.025, 0.2, 0.01, 1.4, 0.99, 3, 0.8, 0.550] #kWh
#perfil=[16, 24, 6, 16, 1, 2, 1, 2, 1, 1]
#resultado: 12.610400 R$/dia, 378.312000 R$/mes, 685 geracoes

#agente07/08 classe alta
#pot=[0.330, 0.2, 0.03, 0.4, 0.01, 2.65, 1.06, 4.5, 1.4, 0.550] #kWh
#perfil=[16, 24, 6, 16, 1, 3, 1, 2, 1, 1]
#resultado: 19.791600 R$/dia, 593.748000 R$/mes, 375 geracoes

#agente09/10 empresa
pot=[0.2, 0.03, 1.4, 0.01, 3.8, 0.550, 1, 5.5, 2, 0.6] #kWh
perfil=[24, 8, 8, 8, 8, 1, 0, 0, 0, 0]
#resultado: 25.324400 R$/dia, 759.732000 R$/mes, 306 geracoes


#MAX_GEN = 1000
TAM_POP = 100
TX_ELITISMO = 0.225 #1-tx_cross
TX_CROSS = 0.775 # 0.6~0.95
TX_MUT = 0.04167 #1/24
#stop=0 #criterio de parada

def GeraPop():
	pop = []
	for i in range(TAM_POP):
		M=[]
		for j in range(24):
			l=[]
			for k in range(10):
				x=random.randint(0,1)
				l.append(x)
			M.append(l)
		pop.append(M)
	return pop

def Aptidao(M):
	#custo em reais por dia
	custo=0
	for i in range(len(M)):
		soma=0
		for j in range(len(M[i])):
			soma=soma+M[i][j]*pot[j]
		custo=custo+soma*preco[i]

	#restricao
	total=[0,0,0,0,0,0,0,0,0,0]
	for i in range(len(M)):
		for j in range(len(M[i])):
			total[j]=total[j]+M[i][j]

	penalidade=[0,0,0,0,0,0,0,0,0,0]
	for i in range(len(total)):
		if total[i]<perfil[i]:
			penalidade[i]=perfil[i]-total[i]

	for i in range(len(total)):
		custo=custo+2*penalidade[i]*pot[i]*max(preco)
	
	return custo

def CalculaAptidoes(pop):
	return [Aptidao(M) for M in pop]

def SelecaoRoleta(aptidoes):
	percentuais = [i/sum(aptidoes) for i in aptidoes]
	percentuais = [1-i for i in percentuais]
	percentuais = [i/sum(percentuais) for i in percentuais]
	vet = [percentuais[0]]
	for p in percentuais[1:]:
		vet.append(vet[-1]+p)
	r = random.random()
	for i in range(len(vet)):
		if r <= vet[i]:
			return i

def Cruzamento(M1,M2):
	filho=[]
	for i in range(24):
		linha=[]
		for j in range(10):
			linha.append(0)
		filho.append(linha)

	for i in range(len(M1)):
		for j in range(len(M1[0])):
			if (M1[i][j]==M2[i][j]):
				filho[i][j]=M1[i][j]
			else:
				filho[i][j]=random.randint(0,1)
	return(filho)

def Mutacao(M):
	i = random.randint(0,len(M)-1)
	j = random.randint(0,len(M[0])-1)
	aux=0
	if M[i][j]==0:
		M[i][j]=1

		while aux==0:
			i = random.randint(0,len(M)-1)
			if M[i][j]==1:
				M[i][j]=0
				aux=1

	elif M[i][j]==1:
		M[i][j]=0

		while aux==0:
			i = random.randint(0,len(M)-1)
			if M[i][j]==0:
				M[i][j]=1
				aux=1

	return M

pop = GeraPop()

medias = []
melhores = []

total=[0,0,0,0,0,0,0,0,0,0]
g=0 #contador geracoes
last_gen = 0 #ultima geracao com atualizacao no valor de menor aptidao

aptidoes = CalculaAptidoes(pop)
aptidao_minima=min(aptidoes)


#while g<=MAX_GEN:
#while total != perfil:
#while 1:
while g-last_gen<=1000: #passadas x geracoes sem descobrir um valor mais otimizado

	aptidoes = CalculaAptidoes(pop)

	if min(aptidoes) < aptidao_minima:
		last_gen = g

	aptidao_minima=min(aptidoes)

	aptidao_media=sum(aptidoes)/len(aptidoes)
	#print(g, "%.2f" %  min(aptidoes), last_gen)
	print(g, min(aptidoes), last_gen)
	print(total)
	nova_pop = []
	g=g+1

	#elitismo
	count=0
	pop2=copy.copy(pop)
	aptidoes2=copy.copy(aptidoes)
	while count<len(pop2)*TX_ELITISMO:
		for i in range(len(pop2)):
			if aptidoes2[i] == min(aptidoes2):
				nova_pop.append(pop2[i])
				aptidoes2.pop(i)
				pop2.pop(i)
				count=count+1
				break

	#cruzemento
	for c in range(TAM_POP-len(nova_pop)):
		pai = pop[SelecaoRoleta(aptidoes)]
		mae = pop[SelecaoRoleta(aptidoes)]
	
		r = random.random()
		if r < TX_CROSS:
			filho = Cruzamento(pai,mae)
		else:
			r = random.random()
			if r>=0.5:
				filho=pai
			else:
				filho=mae

		#mutacao
		r = random.random()
		if r < TX_MUT:
			filho = Mutacao(filho)
		
		nova_pop.append(filho)

	pop = nova_pop
	medias.append(aptidao_media)
	melhores.append(min(aptidoes))

	aptidoes = CalculaAptidoes(pop)
	index_solucao = aptidoes.index(min(aptidoes))
	x=pop[index_solucao]
	for i in range(len(x)):
		print(x[i], i)
	
	total=[0,0,0,0,0,0,0,0,0,0]
	for i in range(len(x)):
		for j in range(len(x[i])):
			total[j]=total[j]+x[i][j]

print('')
print("Concluido!")
print('')

aptidoes = CalculaAptidoes(pop)
index_solucao = aptidoes.index(min(aptidoes))
x=pop[index_solucao]

for i in range(len(x)):
	print(x[i], i)

total=[0,0,0,0,0,0,0,0,0,0]
for i in range(len(x)):
	for j in range(len(x[i])):
		total[j]=total[j]+x[i][j]
print(total)
#print(min(aptidoes), last_gen)
print("resultado: %f R$/dia, %f R$/mes, %d geracoes" % (aptidao_minima, 30*aptidao_minima, last_gen))
