package model

case class RankResponse (
    all: Array[MainDomainInfo],
    black: Array[MainDomainInfo],
    white: Array[MainDomainInfo],
    unknow: Array[MainDomainInfo])